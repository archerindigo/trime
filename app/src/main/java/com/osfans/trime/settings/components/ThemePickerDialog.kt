package com.osfans.trime.settings.components

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import com.osfans.trime.Config
import com.osfans.trime.R
import com.osfans.trime.Trime
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/** 顯示配色方案列表 */
class ThemePickerDialog(private val context: Context): CoroutineScope {
    private val job = Job()
    override val coroutineContext : CoroutineContext
        get() = Dispatchers.Main + job

    private val config = Config.get(context)
    private val themeKeys: Array<String?>
    private val themeNames: Array<String?>
    private var checkedId: Int = 0
    val pickerDialog: AlertDialog
    @Suppress("DEPRECATION")
    private val progressDialog: ProgressDialog

    init {
        val themeFile = config.theme + ".yaml"
        themeKeys = Config.getThemeKeys(context, true)
        themeKeys.sort()
        checkedId = themeKeys.binarySearch(themeFile)

        val themeMap = HashMap<String, String>().apply {
            put("tongwenfeng", context.getString(R.string.pref_themes_name_tongwenfeng))
            put("trime", context.getString(R.string.pref_themes_name_trime))
        }
        val nameArray = Config.getThemeNames(themeKeys)
        themeNames = arrayOfNulls(nameArray.size)

        for (i in nameArray.indices) {
            val themeName = themeMap[nameArray[i]]
            if (themeName == null) {
                themeNames[i] = nameArray[i]
            } else {
                themeNames[i] = themeName
            }
        }
        // Init picker
        pickerDialog = AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_themes)
            setCancelable(true)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(android.R.string.ok) { _, _ -> execute() }
            setSingleChoiceItems(
                themeNames, checkedId
            ) { _, id -> checkedId = id }
        }.create()
        // Init progress dialog
        @Suppress("DEPRECATION")
        progressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.themes_progress))
            setCancelable(false)
        }
    }

    private fun setTheme() { config.theme = themeKeys[checkedId]?.replace(".yaml", "") }
    /** 调用该方法显示对话框 **/
    fun show() = pickerDialog.show()

    private fun execute() = launch {
        onPreExecute()
        doInBackground()
        onPostExecute()
    }

    private fun onPreExecute() = progressDialog.show()

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
        setTheme()
        delay(1000) // Simulate async task
        return@withContext "OK"
    }

    private fun onPostExecute() {
        progressDialog.dismiss()
        Trime.getService()?.initKeyboard() // 實時生效
    }
}