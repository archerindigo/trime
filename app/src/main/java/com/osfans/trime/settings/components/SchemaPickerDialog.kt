package com.osfans.trime.settings.components

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import com.osfans.trime.Function
import com.osfans.trime.R
import com.osfans.trime.Rime
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

class SchemaPickerDialog(private val context: Context): CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var schemaItems: Array<String?>
    private lateinit var checkedStatus: BooleanArray
    private var schemaMapList: List<Map<String?, String?>>? = Rime.get_available_schema_list()
    private lateinit var schemaNames: Array<String?>
    val pickerDialogBuilder: AlertDialog.Builder
    @Suppress("DEPRECATION")
    private var progressDialog: ProgressDialog

    companion object {
        private val CLASS_TAG = SchemaPickerDialog::class.java.simpleName

        private class SortByName: Comparator<Map<String?, String?>> {
            override fun compare(o1: Map<String?, String?>, o2: Map<String?, String?>): Int {
                val s1 = o1["schema_id"]
                val s2 = o2["schema_id"]
                return if (s1 != null && s2 !== null) {
                    s1.compareTo(s2)
                } else -1
            }
        }
    }

    init {
        pickerDialogBuilder = AlertDialog.Builder(context).apply {
            setTitle(R.string.pref_schemas)
            setCancelable(true)
            setPositiveButton(android.R.string.ok, null);
        }
        if (schemaMapList.isNullOrEmpty()) {
            pickerDialogBuilder.setMessage(R.string.no_schemas)
        } else {
            pickerDialogBuilder.apply {
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    @Suppress("DEPRECATION")
                    progressDialog = ProgressDialog(context).apply {
                        setMessage(R.string.deploy_progress)
                        show()
                    }
                    launch {
                        try {
                            setSchema()
                        } catch (e: Exception) {
                            Log.e(CLASS_TAG, "Fail to set schema: $e")
                        } finally {
                            progressDialog.dismiss()
                            exitProcess(0) // 清理内存
                        }
                    }}
                setMultiChoiceItems(
                    schemaNames, checkedStatus
                ) { _, id, isChecked -> checkedStatus[id] = isChecked }

            }
        }
        @Suppress("DEPRECATION")
        progressDialog = ProgressDialog(context).apply {
            setMessage(context.getString(R.string.schemas_progress))
            setCancelable(false)
        }
    }

    private fun initSchemas() {
        if (schemaMapList.isNullOrEmpty()) return
        schemaMapList!!.sortedWith(SortByName())
        val selectedSchemas = Rime.get_selected_schema_list()
        val selectedIds = ArrayList<String>()
        schemaMapList!!.size.let {
            schemaNames = arrayOfNulls(it)
            checkedStatus = BooleanArray(it)
            schemaItems = arrayOfNulls(it)
        }

        if (selectedSchemas.size > 0) {
            for (m in selectedSchemas) {
                m["schema_id"]?.let { selectedIds.add(it) }
            }
        }
        for ((i, m) in schemaMapList!!.withIndex()) {
            schemaNames[i] = m["name"]
            m["schema_id"].let { // schema_id
                schemaItems[i] = it
                checkedStatus[i] = selectedIds.contains(it)
            }
        }
    }

    private fun setSchema() {
        val checkedIds = ArrayList<String>()
        for ((i, b) in checkedStatus.withIndex()) {
            if (b) schemaItems[i]?.let { checkedIds.add(it) }
        }
        if (checkedIds.size > 0) {
            val schemaIdList = arrayOfNulls<String>(checkedIds.size)
            checkedIds.toArray(schemaIdList)
            Rime.select_schemas(schemaIdList)
            Function.deploy(context)
        }
    }
    /** 调用该方法显示对话框 **/
    fun show() = execute()

    private fun execute() = launch {
        onPreExecute()
        doInBackground()
        onPostExecute()
    }

    private fun onPreExecute() = progressDialog.show()

    private suspend fun doInBackground(): String = withContext(Dispatchers.IO) {
        initSchemas()
        delay(1000) // Simulate async task
        return@withContext "OK"
    }

    private fun onPostExecute() {
        progressDialog.dismiss()
        pickerDialogBuilder.create().show()
    }
}