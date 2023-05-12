package digital.paynetics.phos.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import digital.paynetics.phos.R
import digital.paynetics.phos.classes.helpers.IdProvider
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider

fun Intent.tryStartActivity(context: Context) {
    if (resolveActivity(context.packageManager) != null) {
        context.startActivity(this)
    }
}

// More on spans - https://medium.com/androiddevelopers/spantastic-text-styling-with-spans-17b0c16b4568
fun TextView.makeTextClickable(clickableText: String, onClick: () -> Unit) {
    if (clickableText.isBlank()) {
        return
    }

    val clickableSpan = object : ClickableSpan() {
        override fun onClick(p0: View) {
            onClick()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
            ds.color = resources.getColor(R.color.azure_blue)
            ds.typeface = Typeface.DEFAULT_BOLD
        }
    }

    val startIndex = text.indexOf(clickableText)
    if (startIndex == -1) {
        return
    }

    val endIndex = startIndex + clickableText.length

    val spannableString = SpannableString(text)
    spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    text = spannableString
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

fun getVersionName(context: Context, idProvider: IdProvider): String {
    val versionName = idProvider.appName
    val appName = context.getString(R.string.app_name)
    return "$appName $versionName"
}

fun getVersionCode(stringProvider: PhosStringProvider, idProvider: IdProvider): String {
    val versionLabel = stringProvider.getString(PhosString.app_version)
    val versionCode = idProvider.appVersion
    return "$versionLabel: $versionCode"
}

fun copyTextToClipboard(text: String, context: Context, confirmationText: String? = null) {
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))
    // Android 13 and above shows system confirmation message.
    if (confirmationText?.isNotBlank() == true) {
        Toast.makeText(context, confirmationText, Toast.LENGTH_SHORT).show()
    }
}

fun View.setTitleTextView(text: CharSequence, @IdRes viewGroup: Int = 0) {
    findTextView(R.id.title, viewGroup)?.text = text
}

fun View.setValueTextView(text: String, @IdRes viewGroup: Int = 0) {
    findTextView(R.id.value, viewGroup)?.text = text
}

fun View.findTextView(@IdRes view: Int, @IdRes viewGroup: Int = 0): TextView? {
    val rootView = if (viewGroup == 0) id else viewGroup
    val foundView = findViewById<View>(rootView)?.findViewById<View>(view)
    return if (foundView is TextView) foundView else null
}

fun View.makeChildViewGone(@IdRes view: Int, @IdRes viewGroup: Int = 0) {
    setChildViewVisibility(view, viewGroup, View.GONE)
}

fun View.makeChildViewInvisible(@IdRes view: Int, @IdRes viewGroup: Int = 0) {
    setChildViewVisibility(view, viewGroup, View.INVISIBLE)
}

fun View.makeChildViewVisible(@IdRes view: Int, @IdRes viewGroup: Int = 0) {
    setChildViewVisibility(view, viewGroup, View.VISIBLE)
}

fun View.setChildViewVisibility(@IdRes view: Int, @IdRes viewGroup: Int, visibility: Int) {
    val rootView = if (viewGroup == 0) id else viewGroup
    findViewById<View>(rootView)?.findViewById<View>(view)?.visibility = visibility
}

fun setText(
    root: View?,
    @IdRes target: Int,
    phosString: PhosString,
    stringManager: PhosStringProvider
) {
    root?.findViewById<TextView>(target)?.text = stringManager.getString(phosString)
}

fun setTitle(root: View, phosString: PhosString, stringManager: PhosStringProvider) {
    setText(root, R.id.title, phosString, stringManager)
}

fun setHint(root: View, phosString: PhosString, stringManager: PhosStringProvider) {
    setText(root, R.id.hint, phosString, stringManager)
}

fun setClickListener(root: View, listener: View.OnClickListener) {
    setClickListener(root, 0, listener)
}

private fun setClickListener(root: View, @IdRes target: Int, listener: View.OnClickListener) {
    if (target == 0) {
        root.setOnClickListener(listener)
    } else {
        val targetView = root.findViewById<View>(target)
        targetView?.setOnClickListener(listener)
    }
}