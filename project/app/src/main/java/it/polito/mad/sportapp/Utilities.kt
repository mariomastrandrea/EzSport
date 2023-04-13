package it.polito.mad.sportapp

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.WindowMetrics
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import es.dmoral.toasty.Toasty
import org.json.JSONObject
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt

/* Sport utilities */

internal data class Sport(val name: String, var selected: Boolean, var level: Level) {
    companion object {
        fun from(name: String, jsonObject: JSONObject): Sport =
            jsonObject.getJSONObject(name).let {
                Sport(name, it.getBoolean("selected"), Level.of(it.getInt("level")))
            }
    }

    fun saveAsJson(jsonObject: JSONObject) {
        val sportJson = JSONObject()
        sportJson.put("selected", this.selected)
        sportJson.put("level", this.level.ordinal)  // "level" -> 0/1/2/3/4
        jsonObject.put(this.name, sportJson)
    }
}

internal fun extendedNameOf(sportName: String): String = when(sportName) {
    "basket" -> "Basket"
    "soccer5" -> "5-a-side Soccer"
    "soccer8" -> "8-a-side Soccer"
    "soccer11" -> "11-a-side Soccer"
    "tennis" -> "Tennis"
    "tableTennis" -> "Table Tennis"
    "volleyball" -> "Volleyball"
    "beachVolley" -> "Beach Volley"
    "padel" -> "Padel"
    "miniGolf" -> "Mini Golf"
    "swimming" -> "Swimming"
    else -> "????"
}

internal fun getHardcodedSports() = arrayOf(
    Sport("basket", true, Level.EXPERT),
    Sport("tennis", true, Level.BEGINNER)
)

internal class SportChips(
    val name: String, val chip: Chip, val actualLevelChip: Chip)

/* Enum utilities */

internal enum class Gender {
    Male, Female, Other
}

internal enum class Level {
    BEGINNER, INTERMEDIATE, EXPERT, PRO, NO_LEVEL;

    companion object {
        fun of(ordinal: Int): Level = when(ordinal) {
            0 -> BEGINNER
            1 -> INTERMEDIATE
            2 -> EXPERT
            3 -> PRO
            4 -> NO_LEVEL
            else -> throw RuntimeException("It does not exist a Level of $ordinal")
        }
    }

    fun icon() = when(this) {
        BEGINNER -> R.drawable.beginner_level_badge
        INTERMEDIATE -> R.drawable.intermediate_level_badge
        EXPERT -> R.drawable.expert_level_badge
        PRO -> R.drawable.pro_level_badge
        else -> throw RuntimeException("It does not exist such level")
    }
}

/* Display utilities */

/** Returns display width and display height */
internal fun AppCompatActivity.getDisplayMeasures(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val metrics: WindowMetrics = windowManager.currentWindowMetrics
        val displayHeight = metrics.bounds.height()
        val displayWidth = metrics.bounds.width()
        Pair(displayWidth, displayHeight)
    } else {
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val displayHeight = metrics.heightPixels
        val displayWidth = metrics.widthPixels
        Pair(displayWidth, displayHeight)
    }
}

/**
 * Change profile picture size:
 * - set the height to 1/3 of the view (*excluding* the menu) in portrait view
 * - set the width to 1/3 of the view in landscape view
 */
internal fun AppCompatActivity.setProfilePictureSize(
    menuHeight: Int, profilePictureContainer: ConstraintLayout,
    backgroundProfilePicture: ImageView, profilePicture: ImageView
) {
    // retrieve display sizes
    val (displayWidth, displayHeight) = this.getDisplayMeasures()

    // if orientation is vertical, set the picture box height to 1/3 of the display (excluding the menu)
    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        profilePictureContainer.layoutParams.height = (displayHeight - menuHeight) / 3
    }
    // if orientation is horizontal, set the picture box width to 1/3 the display
    else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        profilePictureContainer.layoutParams.width = displayWidth / 3
    }

    // render new dimensions on the screen
    profilePictureContainer.requestLayout()
    backgroundProfilePicture.requestLayout()
    profilePicture.requestLayout()
}

/* manipulate image bitmaps */

/** Transform image into a bitmap */
internal fun uriToBitmap(selectedFileUri: Uri, contentResolver: ContentResolver): Bitmap? {
    try {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
        val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor.close()
        return image
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

/**
 * Rotate image if image captured from samsung devices (Most phone cameras
 * are landscape, meaning if you take the photo in portrait, the resulting
 * photos will be rotated 90 degrees)
 */
internal fun rotateBitmap(
    imageUri: Uri?,
    bitmap: Bitmap,
    contentResolver: ContentResolver
): Bitmap? {
    val input = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
    val exif = ExifInterface(imageUri?.let { contentResolver.openInputStream(it) }!!)
    val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    val rotationMatrix = Matrix()

    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.setRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.setRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.setRotate(270f)
        else -> return bitmap
    }

    return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
}

/* saving picture on internal storage */

internal fun savePictureOnInternalStorage(picture: Bitmap, directory: File, filename: String) {
    val file = File(directory, filename)
    val outputStream = FileOutputStream(file)
    picture.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    outputStream.flush()
    outputStream.close()
}

/* getting picture from internal storage */

internal fun getPictureFromInternalStorage(directory: File, filename: String): Bitmap? {
    val file = File(directory, filename)
    return if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else null
}

/* clearing storage files that match a specific regexp */

internal fun clearStorageFiles(directory: File, regexp: String) {

    // get all files in the cache directory
    val files = directory.listFiles()

    files?.let {
        for (file in it) {
            // delete file if it matches the regexp
            if (file.name.matches(Regex(regexp))) {
                file.delete()
            }
        }
    }
}

/* showing toast according to its type */

internal fun showToasty(type: String, context: Context, message: String) {
    when (type) {
        "success" -> Toasty.custom(context, message,
            ContextCompat.getDrawable(context, R.drawable.baseline_check_24),
            ContextCompat.getColor(context, R.color.toast_success),
            ContextCompat.getColor(context, R.color.white),
            Toasty.LENGTH_SHORT, true, true).show()

        "error"   -> Toasty.custom(context, message,
            ContextCompat.getDrawable(context, R.drawable.outline_close_24),
            ContextCompat.getColor(context, R.color.toast_error),
            ContextCompat.getColor(context, R.color.white),
            Toasty.LENGTH_SHORT, true, true).show()

        "info"    -> Toasty.custom(context, message,
            ContextCompat.getDrawable(context, R.drawable.outline_info_24),
            ContextCompat.getColor(context, R.color.toast_info),
            ContextCompat.getColor(context, R.color.white),
            Toasty.LENGTH_SHORT, true, true).show()

        "warning" -> Toasty.custom(context, message,
            ContextCompat.getDrawable(context, R.drawable.baseline_warning_24),
            ContextCompat.getColor(context, R.color.toast_warning),
            ContextCompat.getColor(context, R.color.white),
            Toasty.LENGTH_SHORT, true, true).show()
    }
}

/**
 * Blur an image bitmap according to the specified parameters
 *
 * Stack Blur Algorithm by Mario Klingemann mario@quasimondo.com
 *
 * This is a compromise between Gaussian Blur and Box blur It creates much
 * better looking blurs than Box Blur, but is 7x faster than my Gaussian
 * Blur implementation.
 */
internal fun fastblur(bitmapParam: Bitmap, scale: Float, radius: Int): Bitmap? {
    var sentBitmap = bitmapParam
    val width = (sentBitmap.width * scale).roundToInt()
    val height = (sentBitmap.height * scale).roundToInt()
    sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
    val bitmap = sentBitmap.copy(sentBitmap.config, true)
    if (radius < 1) {
        return null
    }
    val w = bitmap.width
    val h = bitmap.height
    val pix = IntArray(w * h)
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)
    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1
    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    val vmin = IntArray(w.coerceAtLeast(h))
    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }
    yi = 0
    var yw: Int = yi
    val stack = Array(div) {
        IntArray(
            3
        )
    }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int
    y = 0
    while (y < h) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        i = -radius
        while (i <= radius) {
            p = pix[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            i++
        }
        stackpointer = radius
        x = 0
        while (x < w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (y == 0) {
                vmin[x] = (x + radius + 1).coerceAtMost(wm)
            }
            p = pix[yw + vmin[x]]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi++
            x++
        }
        yw += w
        y++
    }
    x = 0
    while (x < w) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * w
        i = -radius
        while (i <= radius) {
            yi = 0.coerceAtLeast(yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
            i++
        }
        yi = x
        stackpointer = radius
        y = 0
        while (y < h) {

            // Preserve alpha channel: ( 0xff000000 & pix[yi] )
            pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = (y + r1).coerceAtMost(hm) * w
            }
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
            y++
        }
        x++
    }
    Log.e("pix", w.toString() + " " + h + " " + pix.size)
    bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    return bitmap
}

/* measures utilities */

fun Float.dpToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    ).toInt()
}



fun Float.spToPx(context: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        context.resources.displayMetrics
    ).toInt()
}

fun Int.pxToDp(context: Context): Float {
    return this.toFloat() / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}
