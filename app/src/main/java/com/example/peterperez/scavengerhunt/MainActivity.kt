package com.example.peterperez.scavengerhunt

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

/* portions of this code were inspired by or use code from the following guides and projects:

-https://www.andreasjakl.com/nfc-tags-ndef-and-android-with-kotlin/
-https://android.jlelse.eu/writing-to-a-nfc-tag-on-android-8d58f5e3c1fc
-https://expertise.jetruby.com/a-complete-guide-to-implementing-nfc-in-a-kotlin-application-5a94c5baf4dd


 */

class MainActivity : AppCompatActivity() {

    private var nfcPendingIntent: PendingIntent? = null
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        header_text.text = "Your Next Hint is:"


        nfcPendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        if (intent != null) {
            var msg =retrieveNFCMessage(this.intent)
            var msgSplit=msg.split(":")

            hint_text.text=msg
        }


    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            val messageWrittenSuccessfully = createNFCMessage(input_text.text.toString(), intent)

        }
    }



    private fun getNFCMsg(intent: Intent): Array<NdefMessage> {

        val rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        rawMessage?.let {
            return rawMessage.map {
                it as NdefMessage
            }.toTypedArray()
        }
        // Unknown tag type
        val empty = byteArrayOf()
        val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty)
        val msg = NdefMessage(arrayOf(record))
        return arrayOf(msg)
    }


    fun retrieveNFCMessage(intent: Intent?):String {
        var inMessage=""
        val action = intent?.action
        intent?.let {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val nDefMessages = getNFCMsg(intent)
                nDefMessages[0].records?.let {
                    it.forEach {
                        it?.payload.let {
                            it?.let {
                                inMessage=String(it)
                                var inMessage0=inMessage.split(":")
                                return inMessage0[2]
                            }
                        }
                    }
                }
            } else {
                return "Touch NFC tag to read data"
            }
        }
        return inMessage
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val pendingIntent = PendingIntent.getActivity(this, 0,
                    Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
            val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            val filters = arrayOf(nfcIntentFilter)

            val TechLists = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

            nfcAdapter!!.enableForegroundDispatch(this, pendingIntent, filters, TechLists)

        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.let {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    fun createNFCMessage(payload: String, intent: Intent?) : Boolean {

        val pathPrefix = "com:ScavengerHunt"
        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, pathPrefix.toByteArray(), ByteArray(0), payload.toByteArray())
        val nfcMessage = NdefMessage(arrayOf(nfcRecord))
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            return  writeMessageToTag(nfcMessage, tag)
        }
        return false
    }

    private fun writeMessageToTag(nfcMessage: NdefMessage, tag: Tag?): Boolean {

        try {
            val nDefTag = Ndef.get(tag)

            nDefTag?.let {
                it.connect()
                if (it.maxSize < nfcMessage.toByteArray().size) {
                    //Message to large to write to NFC tag
                    return false
                }
                if (it.isWritable) {
                    it.writeNdefMessage(nfcMessage)
                    it.close()
                    //Message is written to tag
                    return true
                } else {
                    //NFC tag is read-only
                    return false
                }
            }

            val nDefFormatableTag = NdefFormatable.get(tag)

            nDefFormatableTag?.let {
                try {
                    it.connect()
                    it.format(nfcMessage)
                    it.close()
                    //The data is written to the tag
                    return true
                } catch (e: IOException) {
                    //Failed to format tag
                    return false
                }
            }
            //NDEF is not supported
            return false

        } catch (e: Exception) {
            //Write operation has failed
        }
        return false
    }


}




