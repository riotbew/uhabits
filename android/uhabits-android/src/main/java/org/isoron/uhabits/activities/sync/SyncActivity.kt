/*
 * Copyright (C) 2016-2020 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.sync

import android.content.*
import android.content.ClipboardManager
import android.graphics.*
import android.os.*
import android.text.*
import android.view.*
import com.google.zxing.*
import com.google.zxing.qrcode.*
import kotlinx.coroutines.*
import org.isoron.androidbase.activities.*
import org.isoron.androidbase.utils.*
import org.isoron.androidbase.utils.InterfaceUtils.getFontAwesome
import org.isoron.uhabits.*
import org.isoron.uhabits.activities.*
import org.isoron.uhabits.core.tasks.*
import org.isoron.uhabits.databinding.*
import org.isoron.uhabits.sync.*


class SyncActivity : BaseActivity() {

    private lateinit var taskRunner: TaskRunner
    private lateinit var baseScreen: BaseScreen
    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private lateinit var binding: ActivitySyncBinding

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        baseScreen = BaseScreen(this)

        val component = (application as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()
        taskRunner = component.taskRunner

        binding = ActivitySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.errorIcon.typeface = getFontAwesome(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.elevation = 10.0f

        binding.instructions.setText(Html.fromHtml(resources.getString(R.string.sync_instructions)))

        binding.syncLink.setOnClickListener {
            copyToClipboard()
        }
    }

    override fun onResume() {
        super.onResume()
        displayLoading()
        taskRunner.execute(object : Task {
            private var key = ""
            private var error = false
            override fun doInBackground() {
                runBlocking {
                    val server = RemoteSyncServer()
                    try {
                        key = server.register()
                    } catch(e: ServiceUnavailable) {
                        error = true
                    }
                }
            }
            override fun onPostExecute() {
                if(error) {
                    displayError()
                } else {
                    displayLink("https://loophabits.org/sync/$key")
                    displayPassword("6B2W9F5X")
                }
            }
        })
    }

    private fun displayLoading() {
        binding.qrCode.visibility = View.GONE
        binding.progress.visibility = View.VISIBLE
        binding.errorPanel.visibility = View.GONE
    }

    private fun displayError() {
        binding.qrCode.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.errorPanel.visibility = View.VISIBLE
    }

    private fun copyToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Loop Sync Link", binding.syncLink.text))
        baseScreen.showMessage(R.string.copied_to_the_clipboard, binding.root)
    }

    private fun displayPassword(pin: String) {
        binding.password.text = pin
    }

    private fun displayLink(link: String) {
        binding.qrCode.visibility = View.VISIBLE
        binding.progress.visibility = View.GONE
        binding.errorPanel.visibility = View.GONE
        binding.syncLink.text = link
        displayQR(link)
    }

    private fun displayQR(msg: String) {
        val writer = QRCodeWriter()
        val matrix = writer.encode(msg, BarcodeFormat.QR_CODE, 1024, 1024)
        val height = matrix.height
        val width = matrix.width
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val bgColor = StyledResources(this).getColor(R.attr.highContrastReverseTextColor)
        val fgColor = StyledResources(this).getColor(R.attr.highContrastTextColor)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (matrix.get(x, y)) fgColor else bgColor
                bitmap.setPixel(x, y, color)
            }
        }
        binding.qrCode.setImageBitmap(bitmap)
    }
}