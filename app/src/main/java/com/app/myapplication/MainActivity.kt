package com.app.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.myapplication.databinding.ActivityMainBinding
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    // Initializes a variable to later track the session ID for a given request.
    var mySessionId = 0
    lateinit var mSplitInstallManager: SplitInstallManager
    val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            // Handle the user's decision. For example, if the user selects "Cancel",
            // you may want to disable certain functionality that depends on the module.
            when(result.resultCode){
                RESULT_OK -> {
                    Toast.makeText(this, "User accept download large module", Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    mSplitInstallManager.cancelInstall(mySessionId)
                }
            }
        }

    // Creates a listener for request status updates.
    private val listener = SplitInstallStateUpdatedListener { state ->
        Toast.makeText(this, "state:${state.status()}", Toast.LENGTH_LONG).show()
        Log.d("testt", "SplitInstallStateUpdatedListener:${state.status()}")
        if (state.sessionId() == mySessionId) {
            // Read the status of the request to handle the state update.
            when (state.status()) {
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    mSplitInstallManager.startConfirmationDialogForResult(state, activityResultLauncher)
                }

                SplitInstallSessionStatus.DOWNLOADING -> {
                    val totalBytes = state.totalBytesToDownload()
                    val progress = state.bytesDownloaded()
                    // Update progress bar.
                    Toast.makeText(
                        this,
                        "DOWNLOADING:${progress}/${totalBytes}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("testt", "DOWNLOADING:${progress}/${totalBytes}")
                }

                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d("testt", "INSTALLED")

                    // After a module is installed, you can start accessing its content or
                    // fire an intent to start an activity in the installed module.
                    // For other use cases, see access code and resources from installed modules.

                    // If the request is an on demand module for an Android Instant App
                    // running on Android 8.0 (API level 26) or higher, you need to
                    // update the app context using the SplitInstallHelper API.

                    startActivity(
                        Intent().setClassName(
                            "com.app.myapplication",
                            "com.app.dynamicfeature1.DynamicFeature1MainActivity"
                        )
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        initViewActions()
    }

    private fun initViews(){
        // Creates an instance of SplitInstallManager.
        mSplitInstallManager = SplitInstallManagerFactory.create(this)
        // Registers the listener.
        mSplitInstallManager.registerListener(listener)

        val installedModules: Set<String> = mSplitInstallManager.installedModules
        var modules = ""
        val test = mutableListOf<String>()
        for(moduleName in installedModules){
            if(modules.isEmpty()){
                modules = moduleName
            }else {
                modules += ", $moduleName"
            }
            test.add(moduleName)
        }
        binding.txtCurrentInstalledModule.text = modules
    }

    private fun initViewActions() {
        binding.btnDownloadFunction.setOnClickListener {
            // Creates a request to install a module.
            val request =
                SplitInstallRequest
                    .newBuilder()
                    // You can download multiple on demand modules per
                    // request by invoking the following method for each
                    // module you want to install.
                    .addModule("dynamicfeature1")
                    .build()
            mSplitInstallManager
                // Submits the request to install the module through the
                // asynchronous startInstall() task. Your app needs to be
                // in the foreground to submit the request.
                .startInstall(request)
                // You should also be able to gracefully handle
                // request state changes and errors. To learn more, go to
                // the section about how to Monitor the request state.
                .addOnSuccessListener { sessionId ->
                    mySessionId = sessionId
                    Toast.makeText(this, "Success: ${sessionId}", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.d("testt","Fail: ${exception.message}")
                    Toast.makeText(this, "Fail: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

        }
    }
}