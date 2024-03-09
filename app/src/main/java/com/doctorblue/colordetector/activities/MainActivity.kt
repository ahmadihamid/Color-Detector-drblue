package com.doctorblue.colordetector.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.solver.widgets.Guideline
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.doctorblue.colordetector.R
import com.doctorblue.colordetector.adapter.ColorAdapter
import com.doctorblue.colordetector.base.BaseActivity
import com.doctorblue.colordetector.database.ColorViewModel
import com.doctorblue.colordetector.dialog.ColorDetailDialog
import com.doctorblue.colordetector.dialog.ColorDialog
import com.doctorblue.colordetector.fragments.ColorsFragment
import com.doctorblue.colordetector.handler.ColorDetectHandler
import com.doctorblue.colordetector.model.UserColor
import com.doctorblue.colordetector.utils.timer
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : BaseActivity() {

    private lateinit var rv_color: RecyclerView;
    private lateinit var btn_pick_color: ImageView;
    private lateinit var btn_add_list_color: ImageView;
    private lateinit var btn_change_camera: ImageView;
    private lateinit var btn_pick_image: ImageView;
    private lateinit var btn_show_camera: ImageView;
    private lateinit var btn_show_colors: ImageView;
    private lateinit var image_view: ImageView;
    private lateinit var camera_preview: PreviewView;
    private lateinit var card_color: MaterialCardView;
    private lateinit var card_color_preview: MaterialCardView;
    private lateinit var txt_hex: TextView;
    private lateinit var pointer: View;
    private lateinit var guideline_top: View;
    private lateinit var guideline_bottom1: View;
    private lateinit var guideline_left: View;
    private lateinit var guideline_right: View;
    private lateinit var layout_top: RelativeLayout;

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 26
        private val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        private const val REQUEST_CODE = 112
    }

    private lateinit var cameraExecutor: ExecutorService

    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(this)
    }

    // Used to bind the lifecycle of cameras to the lifecycle owner
    private val cameraProvider by lazy {
        cameraProviderFuture.get()
    }
    private var isBackCamera = true

    // Select back camera as a default
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private val colorDetectHandler = ColorDetectHandler()

    private var timerTask: Job? = null

    private var currentColor = UserColor()

    private var isImageShown = false

    private var currentColorList: MutableList<UserColor> =
        mutableListOf()

    private val colorAdapter: ColorAdapter by lazy {
        ColorAdapter(this) {
            val detailDialog = ColorDetailDialog(this, it, removeColorInList)
            detailDialog.show()
        }
    }

    private val colorsFragment: ColorsFragment by lazy {
        ColorsFragment()
    }
    private val colorViewModel: ColorViewModel by lazy {
        ViewModelProvider(
            this,
            ColorViewModel.ColorViewModelFactory(application)
        )[ColorViewModel::class.java]
    }

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun initControls(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_main)

        rv_color = findViewById(R.id.rv_color)
        btn_pick_color = findViewById(R.id.btn_pick_color)
        btn_add_list_color = findViewById(R.id.btn_add_list_color)
        btn_change_camera = findViewById(R.id.btn_change_camera)
        btn_pick_image = findViewById(R.id.btn_pick_image)
        btn_show_camera = findViewById(R.id.btn_show_camera)
        btn_show_colors = findViewById(R.id.btn_show_colors)
        image_view = findViewById(R.id.image_view)
        camera_preview = findViewById(R.id.camera_preview)
        card_color = findViewById(R.id.card_color)
        card_color_preview = findViewById(R.id.card_color_preview)
        txt_hex = findViewById(R.id.txt_hex)
        pointer = findViewById(R.id.pointer)
        guideline_top = findViewById(R.id.guideline_top)
        guideline_bottom1 = findViewById(R.id.guideline_bottom1)
        guideline_left = findViewById(R.id.guideline_left)
        guideline_right = findViewById(R.id.guideline_right)
        layout_top = findViewById(R.id.layout_top)

        if (allPermissionsGranted()) {

            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_color.layoutManager = layoutManager
        rv_color.setHasFixedSize(true)
        rv_color.adapter = colorAdapter

    }


    override fun initEvents() {

        btn_pick_color.setOnClickListener {
            addColor()
        }
        btn_add_list_color.setOnClickListener {
            if (currentColorList.isNotEmpty()) {
                val colorDialog = ColorDialog(this, colorViewModel, colorAdapter, clearColorList)
                colorDialog.show()
            }
        }

        btn_change_camera.setOnClickListener {
            if (!isImageShown) {
                if (isBackCamera) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    isBackCamera = false
                } else {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    isBackCamera = true
                }
                startCamera()
            }
        }

        btn_pick_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE)
        }

        btn_show_camera.setOnClickListener {
            if (isImageShown) {
                btn_show_camera.visibility = View.GONE
                image_view.visibility = View.GONE
                isImageShown = false
                startCamera()
            }
        }

        btn_show_colors.setOnClickListener {
            showBottomSheetFragment()
        }

    }


    private fun startCamera() {

        cameraProviderFuture.addListener({

            timerTask?.cancel()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(camera_preview.createSurfaceProvider())
                }

            timerTask = CoroutineScope(Dispatchers.Default).timer(1000) {
                currentColor = colorDetectHandler.detect(camera_preview, pointer)
                Log.d(TAG, "Color : ${currentColor.hex}")

                withContext(Dispatchers.Main) {
                    txt_hex.text = currentColor.hex
                    card_color.setCardBackgroundColor(Color.parseColor(currentColor.hex))
                }
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val x = event.x

            val y = when {
                event.y < guideline_top.y -> guideline_top.y
                event.y > guideline_bottom1.y -> guideline_bottom1.y - pointer.height
                else -> event.y
            }

            setPointerCoordinates(x, y)
        }

        return super.onTouchEvent(event)
    }

    private fun setPointerCoordinates(x: Float, y: Float) {

        pointer.x = x
        pointer.y = y

        val marginBottom = this.resources.getDimension(R.dimen._20sdp)
        card_color_preview.y = y - marginBottom - pointer.height


        val cardColorPreviewX = when {
            x < guideline_left.x ->
                x
            x >= guideline_right.x -> {
                x - card_color_preview.width
            }
            else ->
                x - (card_color_preview.width / 2)
        }

        card_color_preview.x = cardColorPreviewX


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {

            if (!isImageShown) {
                image_view.visibility = View.VISIBLE
                btn_show_camera.visibility = View.VISIBLE
                isImageShown = true
            }
            if (data?.data != null) {
                image_view.setImageURI(data.data)
                startDetectColorFromImage(decodeUriToBitmap(data.data!!))
            }


            /*   Glide.with(this)
                   .asBitmap()
                   .load(data?.data)
                   .into(object : CustomTarget<Bitmap>() {
                       override fun onResourceReady(
                           resource: Bitmap,
                           transition: Transition<in Bitmap>?
                       ) {
                           image_view.setImageBitmap(resource)
                           startDetectColorFromImage(resource)
                       }

                       override fun onLoadCleared(placeholder: Drawable?) = Unit
                   })*/
        }
    }

    private fun startDetectColorFromImage(bitmap: Bitmap) {

        cameraProvider.unbindAll()

        timerTask?.cancel()

        /*  For debugging
          val w = image_view.width
          val h = image_view.height
          */

        // Set Pointer Coordinates at center of image view
        setPointerCoordinates(image_view.width / 2f, image_view.height / 2f)

        /**
         * At this point I don't know how to explain  this code.
         * But I will definitely add it in the future
         */

        var isFitHorizontally = true

        var marginTop: Float = layout_top.height.toFloat()

        var marginLeft = 0f

        val ratio = if (bitmap.width >= bitmap.height) {
            bitmap.width / (image_view.width * 1.0f)
        } else {
            isFitHorizontally = false
            bitmap.height / (image_view.height * 1.0f)
        }


        if (isFitHorizontally) {
            marginTop += (image_view.height - bitmap.height / ratio) / 2
        } else {
            marginLeft += (image_view.width - bitmap.width / ratio) / 2
        }

        timerTask = CoroutineScope(Dispatchers.Default).timer(1000) {

            currentColor =
                colorDetectHandler.detect(bitmap, pointer, marginTop, marginLeft, ratio)
            Log.d(TAG, "Color : ${currentColor.hex}")

            withContext(Dispatchers.Main) {
                txt_hex.text = currentColor.hex
                card_color.setCardBackgroundColor(Color.parseColor(currentColor.hex))
            }
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    private fun addColor() {
        try {
            //Check color before add to list
            Color.parseColor(currentColor.hex)

            colorDetectHandler.convertRgbToHsl(currentColor)
            currentColorList.add(0, currentColor)

            colorAdapter.notifyData(currentColorList)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(
                this,
                resources.getString(R.string.unknown_color),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val clearColorList: () -> Unit = {
        currentColorList.clear()
        colorAdapter.notifyData(currentColorList)
    }

    private fun showBottomSheetFragment() {
        colorsFragment.show(supportFragmentManager, colorsFragment.tag)
    }

    private val removeColorInList: (UserColor) -> Unit = {
        currentColorList.remove(it)
        colorAdapter.notifyData(currentColorList)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerTask?.cancel()
        cameraProvider.unbindAll()
        cameraExecutor.shutdown()
    }

    private fun decodeUriToBitmap(uri: Uri): Bitmap = try {
        val inputStream = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        (image_view.drawable as BitmapDrawable).bitmap
    }

}