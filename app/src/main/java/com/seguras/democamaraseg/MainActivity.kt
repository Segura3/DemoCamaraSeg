package com.seguras.democamaraseg

import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.seguras.democamaraseg.databinding.ActivityMainBinding
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var outputDirectory: File
    private var imageCapture: ImageCapture?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        requestPermissions()

        binding.btnTomarFoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamara(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            }catch (e:Exception){
                Log.d(Constants.TAG, "Error al inicializar la camara")
            }
        },ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(){
        val imageCapture = imageCapture?:return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault())
                .format(System.currentTimeMillis())+".jpg"
        )
        val outputOption = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()
        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Foto guardada"
                    Log.i(Constants.TAG, "Foto: $msg, $savedUri")
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }

    private fun getOutputDirectory():File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            mFile->
            File(mFile,"democamaraseg").apply {
                mkdirs()
            }
        }
        return if(mediaDir!=null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun requestPermissions(){
        if(allPermissionGranted()){
            //Tenemos permiso, podemos inicializar la camara
            startCamara()
        }else{
            ActivityCompat.requestPermissions(this, Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                //Inicializar la camara
                startCamara()
            }else{
                finish()
            }
        }
    }

    private fun allPermissionGranted()=
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext,it)==PackageManager.PERMISSION_GRANTED
        }
}