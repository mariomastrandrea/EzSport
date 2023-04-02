package it.polito.mad.lab2

import android.Manifest
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import java.io.*


class EditProfileActivity : AppCompatActivity() {
    // user info fields' temporary state
    private var firstNameTemp: String? = null
    private var lastNameTemp: String? = null
    private var usernameTemp: String? = null
    private var ageTemp: String? = null
    private var radioGenderCheckedTemp = R.id.radio_male
    private var locationTemp: String? = null
    private var bioTemp: String? = null
    
    // User info views
    private lateinit var firstName: EditText
    private lateinit var lastName: EditText
    private lateinit var username: EditText
    private lateinit var age: EditText
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var location: EditText
    private lateinit var bio: EditText
    
    // Profile picture 
    private lateinit var profilePicture: ImageView
    private var inputImage: Bitmap? = null
    private var galleryUri: Uri? = null
    private var cameraUri: Uri? = null

    /* results launchers */
    private var galleryActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                val data: Intent? = it.data
                galleryUri = data?.data
                inputImage = galleryUri?.let { it1 -> uriToBitmap(it1, contentResolver) }

                Glide.with(this)
                    .asBitmap()
                    .load(inputImage)
                    .override(getDisplayMeasures().first, getDisplayMeasures().second / 3)
                    .centerCrop()
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            profilePicture.setImageBitmap(resource)
                            savePictureOnSharedPreferences(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }

    private var cameraActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                inputImage = cameraUri?.let { it1 -> uriToBitmap(it1, contentResolver) }
                inputImage = inputImage?.let { it1 -> rotateBitmap(cameraUri, it1, contentResolver) }

                //Setting picture into the imageView
                profilePicture.setImageBitmap(inputImage)
            }
        }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // initialize the EditText views
        firstName = findViewById(R.id.edit_first_name)
        lastName = findViewById(R.id.edit_last_name)
        username = findViewById(R.id.edit_username)
        genderRadioGroup = findViewById(R.id.radio_gender_group)
        age = findViewById(R.id.edit_age)
        location = findViewById(R.id.edit_location)
        bio = findViewById(R.id.edit_bio)
        profilePicture = findViewById(R.id.profile_picture)

        // set context menu to change profile picture
        val profileImageButton: ImageButton = findViewById(R.id.profile_picture_button)
        registerForContextMenu(profileImageButton)
        profileImageButton.setOnClickListener {
            // open the related context menu
            openContextMenu(profileImageButton)
        }

        // load data from sharedPreferences file
        this.loadDataFromSharedPreferences()

        // add listeners to the temporary variables
        firstName.addTextChangedListener(textListenerInit("firstName"))
        lastName.addTextChangedListener(textListenerInit("lastName"))
        username.addTextChangedListener(textListenerInit("username"))
        age.addTextChangedListener(textListenerInit("age"))
        location.addTextChangedListener(textListenerInit("location"))
        bio.addTextChangedListener(textListenerInit("bio"))

        genderRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            radioGenderCheckedTemp = checkedId
        }
    }

    private fun loadDataFromSharedPreferences() {
        // retrieve data from SharedPreferences
        val sh = getSharedPreferences("it.polito.mad.lab2", MODE_PRIVATE)

        val firstNameResume = sh.getString("firstName", getString(R.string.first_name))
        val lastNameResume = sh.getString("lastName", getString(R.string.last_name))
        val usernameResume = sh.getString("username", getString(R.string.username))
        val radioCheckedResume = sh.getInt("radioChecked", R.id.radio_male)
        val ageResume = sh.getString("age", getString(R.string.user_age))
        val locationResume = sh.getString("location", getString(R.string.user_location))
        val bioResume = sh.getString("bio", getString(R.string.user_bio))

        val profilePictureResume = sh.getString("profilePicture", null)

        // set EditText views
        firstName.setText(firstNameResume)
        lastName.setText(lastNameResume)
        username.setText(usernameResume)
        genderRadioGroup.check(radioCheckedResume)
        age.setText(ageResume)
        location.setText(locationResume)
        bio.setText(bioResume)

        // set temporary variables
        firstNameTemp = firstNameResume
        lastNameTemp = lastNameResume
        usernameTemp = usernameResume
        radioGenderCheckedTemp = radioCheckedResume
        ageTemp = ageResume
        locationTemp = locationResume
        bioTemp = bioResume

        if (profilePictureResume != null && !profilePictureResume.equals("", ignoreCase = true)) {
            val b: ByteArray = Base64.decode(profilePictureResume, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(b, 0, b.size)
            profilePicture.setImageBitmap(bitmap)
        }
    }

    /* save and restore temporary state */

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save temporary variables into the bundle in order
        // to have the right values once the activity restores
        outState.putString("firstNameTemp", firstNameTemp)
        outState.putString("lastNameTemp", lastNameTemp)
        outState.putString("usernameTemp", usernameTemp)
        outState.putString("ageTemp", ageTemp)
        outState.putInt("radioGenderChecked", radioGenderCheckedTemp)
        outState.putString("locationTemp", locationTemp)
        outState.putString("bioTemp", bioTemp)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Restore temporary variables from the bundle
        firstNameTemp = savedInstanceState.getString("firstNameTemp").toString()
        firstName.setText(firstNameTemp)

        lastNameTemp = savedInstanceState.getString("lastNameTemp").toString()
        lastName.setText(lastNameTemp)

        usernameTemp = savedInstanceState.getString("usernameTemp").toString()
        username.setText(usernameTemp)

        ageTemp = savedInstanceState.getString("ageTemp").toString()
        age.setText(ageTemp)

        radioGenderCheckedTemp = savedInstanceState.getInt("radioGenderChecked")
        genderRadioGroup.check(radioGenderCheckedTemp)

        locationTemp = savedInstanceState.getString("locationTemp").toString()
        location.setText(locationTemp)

        bioTemp = savedInstanceState.getString("bioTemp").toString()
        bio.setText(bioTemp)
    }

    private fun textListenerInit(fieldName: String): TextWatcher {
        // implement and return the TextWatcher interface
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                when (fieldName) {
                    "firstName" -> firstNameTemp = firstName.text.toString()
                    "lastName" -> lastNameTemp = lastName.text.toString()
                    "username" -> usernameTemp = username.text.toString()
                    "age" -> ageTemp = age.text.toString()
                    "location" -> locationTemp = location.text.toString()
                    "bio" -> bioTemp = bio.text.toString()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    /* app menu */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // inflate and render the menu
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.edit_profile_menu, menu)
        // change app bar's title
        supportActionBar?.title = "Edit Profile"

        val menuHeight = supportActionBar?.height!!
        val profilePictureContainer = findViewById<ConstraintLayout>(R.id.profile_picture_container)
        val backgroundProfilePicture = findViewById<ImageView>(R.id.background_profile_picture)
        val profilePicture = findViewById<ImageView>(R.id.profile_picture)

        // set profile picture height 1/3 of the app view
        this.setProfilePictureSize(menuHeight, profilePictureContainer, backgroundProfilePicture, profilePicture)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        // detect when the user clicks on the "confirm" button
        R.id.confirm_button -> {
            // if the user clicks on the confirm button, the temporary information is *saved*
            // into the sharedPreferences file
            val sh = getSharedPreferences("it.polito.mad.lab2", MODE_PRIVATE)
            val editor = sh.edit()

            editor.putString("firstName", firstNameTemp)
            editor.putString("lastName", lastNameTemp)
            editor.putString("username", usernameTemp)
            editor.putString("age", ageTemp)
            editor.putInt("radioChecked", radioGenderCheckedTemp)
            editor.putString("location", locationTemp)
            editor.putString("bio", bioTemp)

            // manage the Gender field to display it correctly in the ShowProfileActivity
            when (radioGenderCheckedTemp) {
                R.id.radio_female -> editor.putString("gender", "Female")
                R.id.radio_other -> editor.putString("gender", "Other")
                else -> editor.putString("gender", "Male")
            }

            // save the picture into the sharedPreferences file
            if (inputImage != null) {
                savePictureOnSharedPreferences(inputImage!!)
            }

            // apply changes and show a pop up to the user
            editor.apply()
            Toast.makeText(this, "Information successfully saved!", Toast.LENGTH_LONG).show()

            // terminate this activity (go back to the previous one)
            this.finish()
            true
        }
        // detect when the user clicks on the "back" button
        R.id.back_button -> {
            // if the user clicks the back button, the temporary information is *not* saved:
            // terminate this activity (go to the previous one)
            this.finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    /* context menu (to choose how to change profile picture) */

    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater: MenuInflater = menuInflater

        when (v.id) {
            R.id.profile_picture_button -> {
                inflater.inflate(R.menu.profile_picture_context_menu, menu)
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        // detect which item has been selected by the user in the 'change profile picture' menu
        return when (item.itemId) {
            R.id.camera -> {
                // check if the permissions are granted
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    // NOTE: the request code is used to identify the request
                    // in the callback function but it is completely random
                    requestPermissions(permission, 112)
                    onRequestPermissionsResult(112, permission, intArrayOf(0, 0))
                }
                else openCamera()
                true
            }
            R.id.gallery -> {
                // check if the permissions are granted
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                ) {
                    val permission = arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    requestPermissions(permission, 113)
                    onRequestPermissionsResult(113, permission, intArrayOf(0, 0))
                }
                else {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryActivityResultLauncher.launch(galleryIntent)
                }

                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 112 && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else if (requestCode == 113 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val galleryIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityResultLauncher.launch(galleryIntent)
        }

    }

    private fun openCamera() {

        // Uncomment these lines if you want to save images inside the phone gallery
        /*
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        cameraUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        */

        // Creating a file object for the temporal image
        val imageFile = File.createTempFile("temp_profile_picture", ".jpeg", cacheDir)

        // Creating through a FileProvider the URI
        cameraUri = FileProvider.getUriForFile(
            this,
            "it.polito.mad.lab2.fileprovider", imageFile
        )

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)

        cameraActivityResultLauncher.launch(cameraIntent)
    }

    /* picture saving */

    private fun savePictureOnInternalStorage(picture: Bitmap) {
        val cw = ContextWrapper(applicationContext)

        val directory: File = cw.getDir("imageDir", MODE_PRIVATE)

        val file = File(directory, "profile_picture" + ".jpg")

        if (!file.exists()) {
            val fos: FileOutputStream?

            try {
                fos = FileOutputStream(file)
                picture.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun savePictureOnSharedPreferences(picture: Bitmap) {
        val sh = getSharedPreferences("it.polito.mad.lab2", MODE_PRIVATE)
        val editor = sh.edit()

        //Encoding bitmap into Base64 string
        val baos = ByteArrayOutputStream()
        picture.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b: ByteArray = baos.toByteArray()
        val encodedImage: String = Base64.encodeToString(b, Base64.DEFAULT)

        // Saving Base64 string into shared preferences
        editor.putString("profilePicture", encodedImage)
        editor.apply()
    }
}
