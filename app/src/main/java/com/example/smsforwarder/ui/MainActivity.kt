package com.example.smsforwarder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smsforwarder.R
import com.example.smsforwarder.email.DefaultEmailForwarder
import com.example.smsforwarder.email.EmailConfig
import com.example.smsforwarder.email.EmailConfigStore
import com.example.smsforwarder.email.EmailForwarder
import com.example.smsforwarder.email.JavaMailEmailTransport
import com.example.smsforwarder.email.SmsData
import com.example.smsforwarder.service.KeepAliveService
import com.example.smsforwarder.util.BatteryOptimizationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvServiceStatus: TextView
    private lateinit var tvBatteryOptStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnRequestBatteryOpt: Button
    
    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etRecipient: EditText
    private lateinit var cbUseTls: CheckBox
    private lateinit var btnSaveConfig: Button
    private lateinit var btnTestEmail: Button

    var emailForwarder: EmailForwarder = DefaultEmailForwarder(JavaMailEmailTransport())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvBatteryOptStatus = findViewById(R.id.tvBatteryOptStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnRequestBatteryOpt = findViewById(R.id.btnRequestBatteryOpt)
        
        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etRecipient = findViewById(R.id.etRecipient)
        cbUseTls = findViewById(R.id.cbUseTls)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)
        btnTestEmail = findViewById(R.id.btnTestEmail)

        checkAndRequestPermissions()
        loadConfigToUi()

        btnToggleService.setOnClickListener {
            if (KeepAliveService.isRunning) {
                KeepAliveService.stopService(this)
            } else {
                KeepAliveService.startService(this)
            }
            updateUiState()
        }

        btnRequestBatteryOpt.setOnClickListener {
            if (!BatteryOptimizationUtil.isIgnoringBatteryOptimizations(this)) {
                val intent = BatteryOptimizationUtil.createRequestIgnoreBatteryOptimizationIntent(this)
                startActivity(intent)
            }
        }
        
        btnSaveConfig.setOnClickListener {
            saveConfigFromUi()
        }

        btnTestEmail.setOnClickListener {
            val config = EmailConfigStore.getConfig(this)
            if (config.username == "user@example.com" || config.password.isBlank()) {
                Toast.makeText(this, "请先设置并保存有效的邮箱配置！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Toast.makeText(this, "正在发送测试邮件，请稍候...", Toast.LENGTH_SHORT).show()
            val testSms = SmsData(
                sender = "测试终端",
                body = "这是一封来自 Android 短信转发工具的测试邮件。如果您看到此邮件，说明配置成功！",
                timestamp = System.currentTimeMillis()
            )
            CoroutineScope(Dispatchers.Main).launch {
                val result = withContext(Dispatchers.IO) {
                    emailForwarder.forwardSms(testSms, config)
                }
                if (result.isSuccess) {
                    Toast.makeText(this@MainActivity, "测试邮件发送成功", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(this@MainActivity, "测试邮件发送失败: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun loadConfigToUi() {
        val config = EmailConfigStore.getConfig(this)
        if (config.smtpHost != "smtp.gmail.com") {
            etHost.setText(config.smtpHost)
        }
        etPort.setText(config.smtpPort.toString())
        if (config.username != "user@example.com") {
            etUsername.setText(config.username)
            etRecipient.setText(config.toAddress)
        }
        if (config.password != "password") {
            etPassword.setText(config.password)
        }
        cbUseTls.isChecked = config.useTls
    }
    
    private fun saveConfigFromUi() {
        val host = etHost.text.toString().trim()
        val portStr = etPort.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val recipient = etRecipient.text.toString().trim()
        val useTls = cbUseTls.isChecked
        
        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty() || password.isEmpty() || recipient.isEmpty()) {
            Toast.makeText(this, "请完整填写所有邮箱配置项", Toast.LENGTH_SHORT).show()
            return
        }
        
        val port = portStr.toIntOrNull() ?: 587
        val config = EmailConfig(
            smtpHost = host,
            smtpPort = port,
            username = username,
            password = password,
            fromAddress = username,
            toAddress = recipient,
            useTls = useTls,
            useSsl = port == 465
        )
        EmailConfigStore.saveConfig(this, config)
        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    fun updateUiState() {
        val isRunning = KeepAliveService.isRunning
        val isWhitelisted = BatteryOptimizationUtil.isIgnoringBatteryOptimizations(this)

        tvBatteryOptStatus.text = if (isWhitelisted) {
            "电池优化：已加入白名单（后台免杀）"
        } else {
            "电池优化：已开启（存在被杀风险）"
        }

        tvServiceStatus.text = if (isRunning) {
            "保活服务：运行中"
        } else {
            "保活服务：已停止"
        }

        btnToggleService.text = if (isRunning) {
            "停止保活服务"
        } else {
            "启动保活服务"
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }
}
