
package com.tanercuhadar.yemekkitabi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.tanercuhadar.yemekkitabi.databinding.FragmentTarifBinding
import com.tanercuhadar.yemekkitabi.model.Tarif
import com.tanercuhadar.yemekkitabi.roomdb.TarifDAO
import com.tanercuhadar.yemekkitabi.roomdb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream

class TarifFragment : Fragment() {
    private var _binding: FragmentTarifBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var secilenGorsel: Uri? = null
    private var secilenBitmap: Bitmap? = null
    private lateinit var db: TarifDatabase
    private lateinit var tarifDao: TarifDAO
    private val mDisposable = CompositeDisposable()
    private var secilenTarif : Tarif? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()
        db = Room.databaseBuilder(requireContext(), TarifDatabase::class.java, name = "Tarifler").build()
        tarifDao = db.tarifDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageView.setOnClickListener { gorselSec(it) }
        binding.silButton.setOnClickListener { sil(it) }
        binding.kaydetButton.setOnClickListener { kaydet(it) }

        arguments?.let {
            val bilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if (bilgi == "yeni") {
                binding.silButton.isEnabled = false
                binding.kaydetButton.isEnabled = true
                binding.isimText.setText("")
                binding.malzemeText.setText("")
            } else {
                binding.silButton.isEnabled = true
                binding.kaydetButton.isEnabled = false
                val id = TarifFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    tarifDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { tarif -> handleRsponse(tarif) },
                            { throwable ->
                                throwable.printStackTrace()
                                Toast.makeText(requireContext(), "Hata: ${throwable.message}", Toast.LENGTH_LONG).show()
                            }
                        )
                )
            }
        }
    }

    private fun handleRsponse(tarif: Tarif) {
        binding.isimText.setText(tarif.isim)
        binding.malzemeText.setText(tarif.malzeme)
        val bitmap = BitmapFactory.decodeByteArray(tarif.gorsel, 0, tarif.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)
        secilenTarif = tarif
    }

    private fun gorselSec(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intentToGallery)
        } else {
            if (shouldShowRequestPermissionRationale(permission)) {
                Snackbar.make(view, "Galeriye ulaşmak için izne ihtiyacımız var!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin ver") { permissionLauncher.launch(permission) }.show()
            } else {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun registerLauncher() {
        galleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val intentFromResult = result.data
                    if (intentFromResult != null) {
                        secilenGorsel = intentFromResult.data
                        try {
                            secilenGorsel?.let {
                                secilenBitmap = if (Build.VERSION.SDK_INT >= 28) {
                                    val source = ImageDecoder.createSource(requireActivity().contentResolver, it)
                                    ImageDecoder.decodeBitmap(source)
                                } else {
                                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                                }
                                binding.imageView.setImageBitmap(secilenBitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(intentToGallery)
                } else {
                    Toast.makeText(requireContext(), "İzin verilmedi!", Toast.LENGTH_LONG).show()
                }
            }
    }

    fun kaydet(view: View) {
        val isim = binding.isimText.text.toString()
        val malzeme = binding.malzemeText.text.toString()
        if (secilenBitmap != null) {
            val kucukBitmap = KucukBitmapOlustur(secilenBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteDizisi = outputStream.toByteArray()
            val tarif = Tarif(isim, malzeme, byteDizisi)

            mDisposable.add(
                tarifDao.insert(tarif)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { handleREsponseForInsert() },
                        { throwable ->
                            throwable.printStackTrace()
                            Toast.makeText(requireContext(), "Kaydetme sırasında bir hata oluştu: ${throwable.message}", Toast.LENGTH_LONG).show()
                        }
                    )
            )
        }
    }

    private fun handleREsponseForInsert() {
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun KucukBitmapOlustur(kullanıcınınSectigiBitmap: Bitmap, maximimBoyut: Int): Bitmap {
        var width = kullanıcınınSectigiBitmap.width
        var height = kullanıcınınSectigiBitmap.height
        val bitmapOranı: Double = width.toDouble() / height.toDouble()
        if (bitmapOranı >= 1) {
            width = maximimBoyut
            height = (width / bitmapOranı).toInt()
        } else {
            height = maximimBoyut
            width = (height * bitmapOranı).toInt()
        }
        return Bitmap.createScaledBitmap(kullanıcınınSectigiBitmap, width, height, true)
    }

    fun sil(view: View) {
        if (secilenTarif!=null){
            mDisposable.add(
                tarifDao.delete(tarif=secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleREsponseForInsert)
            )
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}

