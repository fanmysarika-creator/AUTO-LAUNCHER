package com.autolauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cfg", MODE_PRIVATE)
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(px(16), px(16), px(16), px(16)) }

        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val settings = Button(this).apply {
            text = "Настройки"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val stop = Button(this).apply {
            text = "Стоп"
            setOnClickListener { AutomationService.stop(); toast("Остановлено") }
        }
        top.addView(settings, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(stop, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(top)

        root.addView(TextView(this).apply {
            text = "Включи «AutoLauncher» в Специальных возможностях (кнопка Настройки), затем выбери автоматизацию и жми «Запустить»."
            setPadding(0, px(8), 0, px(8))
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, px(8), 0, px(8)) }
            row.addView(TextView(this).apply { text = ri.loadLabel(pm); textSize = 17f })

            val options = listOf("Без автоматизации") + Scenarios.all.map { it.title }
            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, options)
                val saved = Scenarios.byId(prefs.getString("auto_$pkg", "") ?: "")
                setSelection(if (saved == null) 0 else options.indexOf(saved.title))
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        prefs.edit().putString("auto_$pkg", if (pos == 0) "" else Scenarios.all[pos - 1].id).apply()
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }
            }
            row.addView(spinner)

            row.addView(Button(this).apply {
                text = "Запустить"
                setOnClickListener {
                    val sc = Scenarios.byId(prefs.getString("auto_$pkg", "") ?: "")
                    val launch = pm.getLaunchIntentForPackage(pkg)
                    if (launch == null) { toast("Не удалось открыть"); return@setOnClickListener }
                    startActivity(launch)
                    if (sc != null) {
                        if (AutomationService.instance == null) toast("Включи сервис в Настройках")
                        else AutomationService.run(sc)
                    }
                }
            })
            list.addView(row)
        }

        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
