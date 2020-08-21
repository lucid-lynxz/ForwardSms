package org.lynxz.forwardsms.ui.trans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

// 导入 android X 使用如下导包
import androidx.core.content.FileProvider
import androidx.appcompat.app.AppCompatActivity

// 导入普通support库时启用以下库
//import android.support.v4.content.FileProvider
//import android.support.v7.app.AppCompatActivity

/**
 * Created by lynxz on 2019/4/1
 * E-mail: lynxz8866@gmail.com
 *
 * Description: 封装选择图片逻辑
 * 使用:
 *  1.  在 `app/res/xml/` 目录下创建文件 `provider_paths.xml`,其内容为:
 *  ```xml
 *  <?xml version="1.0" encoding="utf-8"?>
 *       <paths xmlns:android="http://schemas.android.com/apk/res/android">
 *       <external-path
 *          name="external_files"
 *          path="."/>
 *       <external-files-path
 *          name="external_storage_directory"
 *          path="."/>
 *  </paths>
 *  ```
 * 2. 修改 `AndroidManifest.xml` ,增加如下:
 *  <provider
 *      android:name="android.support.v4.content.FileProvider"
 *      android:authorities="${applicationId}.fileProvider"
 *      android:exported="false"
 *      android:grantUriPermissions="true">
 *      <meta-data
 *          android:name="android.support.FILE_PROVIDER_PATHS"
 *          android:resource="@xml/provider_paths"/>
 * </provider>
 *
 *  并添加权限权限:
 *  android.Manifest.permission.WRITE_EXTERNAL_STORAGE
 *  android.Manifest.permission.CAMERA
 *
 *  3. 设置参数:
 *    val frag = BaseTransFragment.getTransFragment(activity!!, "choose_picture_frag", ChoosePictureFragment())
 *    frag?.setCropEnable() // 启用裁剪
 *    ?.setCropSize(200, 200) // 设置裁剪尺寸
 *    ?.setChooseCallback(object : ChoosePictureFragment.IChooseCallback { // 设置回调
 *          override fun onPhotoProcessFinish(photoPath: String?) {
 *          }
 *    })
 *
 * 4. 外部自行申请动态权限成功后,调用如下方法选择图片
 *  [selectFromAlbum] 通过相册选择图片
 *  [selectFromCamera] 通过相机拍照获取图片
 */
class ChoosePictureFragment : BaseTransFragment() {
    companion object {
        private const val CODE_FROM_ALBUM = 1000 // 从相册选取照片
        private const val CODE_FROM_CAMERA = 1010 // 从相机拍摄照片
        private const val CODE_FOR_CUT = 1100 // 裁剪
    }

    private var mHostActivity: Activity? = null
    private var cameraFile: File? = null
    private var cameraUri: Uri? = null
    private var mCallback: IChooseCallback? = null
    private var mPhotoSavePath = ""

    private var enableCrop = false
    private var cropSizeDefault = 204
    private var cropSizeW = cropSizeDefault
    private var cropSizeH = cropSizeDefault

    interface IChooseCallback {
        /**
         * 图片选择完成(裁剪)后回调
         * @param photoPath 最终图片路径
         * */
        fun onPhotoProcessFinish(photoPath: String?)
    }

    /**
     * 设置选择图片回调
     * */
    fun setChooseCallback(callback: IChooseCallback?): ChoosePictureFragment {
        mCallback = callback
        return this
    }

    /**
     * 启用裁剪,默认不裁剪
     * 启用后,从相册/相机完成图片选择后会自动进入裁剪页面
     * */
    fun setCropEnable(): ChoosePictureFragment {
        enableCrop = true
        return this
    }

    /**
     * 设置裁剪尺寸
     * */
    fun setCropSize(width: Int, height: Int): ChoosePictureFragment {
        cropSizeW = if (width > 0) width else cropSizeDefault
        cropSizeH = if (height > 0) height else cropSizeDefault
        return this
    }


    private val mFileProviderAuthority by lazy {
        "${mHostActivity?.packageName}.fileProvider"
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mHostActivity = activity
    }

    override fun onDetach() {
        super.onDetach()
        mHostActivity = null
    }

    /**
     * 从图库选择图片
     */
    fun selectFromAlbum() {
        val intent = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
        } else {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        startActivityForResult(intent, CODE_FROM_ALBUM)
    }

    /**
     * 从图库选择视频
     */
    fun selectVideoFromAlbum() {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
        }, CODE_FROM_ALBUM)
    }

    /**
     * 照相获取图片
     */
    fun selectFromCamera() {
        if (mHostActivity == null) {
            return
        }

        // 拍照完成后,将照片存入指定路径
        cameraFile = File(getImgDir(mHostActivity), "${System.currentTimeMillis()}.png")
        cameraFile?.parentFile?.mkdirs()

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraUri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(cameraFile)
        } else {
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            FileProvider.getUriForFile(mHostActivity!!, mFileProviderAuthority, cameraFile!!)
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
        startActivityForResult(intent, CODE_FROM_CAMERA)
    }

    /**
     * 裁剪图片
     * @param uri 图片路径
     * @param size 裁剪框尺寸,正方形,默认为 204px*204px
     */
    fun startPhotoZoom(uri: Uri?) {
        if (uri == null) {
            return
        }

        val intent = Intent("com.android.camera.action.CROP")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(uri, "image/*")

        // crop为true是设置在开启的intent中设置显示的view可以剪裁
        intent.putExtra("crop", "true")

        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 100)
        // intent.putExtra("aspectY", 100)
        intent.putExtra("aspectY", (cropSizeH * 100) / cropSizeW)

        // outputX,outputY 是剪裁图片的宽高
        intent.putExtra("outputX", cropSizeW)
        intent.putExtra("outputY", cropSizeH)

        // true表示将裁剪后的图片放在data中返回,但通过intent携带的数据量有限制, 超过1M可能会卡
        intent.putExtra("return-data", false)

        // 把图片路径保存到手机中
        mPhotoSavePath = generatePhotoPath()
        val savePhotoFile = File(mPhotoSavePath)
        val tUri = Uri.fromFile(savePhotoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, tUri)

        // 设置裁剪图片格式
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString())
        intent.putExtra("noFaceDetection", true) // 取消人脸识别
        startActivityForResult(intent, CODE_FOR_CUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        doOnActivityResult(requestCode, resultCode, data)
    }

    fun doOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (mHostActivity == null) return

        if (resultCode != AppCompatActivity.RESULT_CANCELED) {
            when (requestCode) {
                CODE_FROM_CAMERA -> {
                    try {
                        var fileUri: Uri? = null
                        cameraFile?.let {
                            val targetPath = it.absolutePath
                            // 请在子线程中自行处理
//                            val degree = imageUtil.getBitmapDegree(targetPath)
                            // 旋转后保存图片暂定最大1M
//                            if (degree != 0 && isSamsungMobile()) {
//                                imageUtil.rotateBitmapByDegree(targetPath, targetPath, degree, 1000)
//                            }
                            fileUri = FileProvider.getUriForFile(
                                mHostActivity!!,
                                mFileProviderAuthority,
                                File(targetPath)
                            )
//                            Logger.d("$degree 旋转角度:  ${imageUtil.getBitmapDegree(file.absolutePath)}  ${imageUtil.getBitmapDegree(targetPath)}")
                        }

                        val uri = if (isSamsungMobile())
                            fileUri ?: cameraUri
                        else
                            cameraUri ?: fileUri

//                        Logger.d("请求相机成功\nCameraUri：${mPicOperation.cameraUri} \nCameraFile： ${mPicOperation.cameraFile}\n最终进行裁剪的Uri: $uri}")
                        if (enableCrop) {
                            startPhotoZoom(uri)
                        } else {
                            mCallback?.onPhotoProcessFinish(cameraFile?.absolutePath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                CODE_FROM_ALBUM -> {
                    if (data != null) {
                        val uri = data.data
                        if (enableCrop) {
                            startPhotoZoom(uri)
                        } else {
                            mCallback?.onPhotoProcessFinish(getPathFromURI(uri))
                        }
                    }
                }
                CODE_FOR_CUT -> {
                    if (data != null) {
                        mCallback?.onPhotoProcessFinish(mPhotoSavePath)
                    }
                }
            }
        }
    }

    private fun generatePhotoPath(): String {
        return "${getImgDir(mHostActivity)}${File.separator}iconUrl_${System.currentTimeMillis()}.png"
    }

    /**
     * 获取图片的缓存的路径
     */
    fun getImgDir(context: Context?): File {
        return getDir(context, "imgCache")
    }

    /**
     * 创建/获取图片缓存所在目录
     * */
    fun getDir(context: Context?, cache: String = "imgCache"): File {
        val path = StringBuilder()
        if (isSDAvailable()) {
            path.append(Environment.getExternalStorageDirectory().absolutePath)
            path.append(File.separator)// '/'
            path.append(cache)// /mnt/sdcard/cache
        } else {
            path.append(context?.cacheDir?.absolutePath)// /Data/Data/com.soundbus.uplusgo/cache
            path.append(File.separator)///mData/mData/com.isoundbus.uplusgo/cache/
            path.append(cache)///mData/mData/com.soundbus.uplusgo/cache/cache
        }

        val file = File(path.toString())
        if (!file.exists() || !file.isDirectory) {
            file.mkdirs()// 创建文件夹
        }
        return file
    }

    /**
     * 通过uri获取照片路径
     */
    private fun getPathFromURI(contentUri: Uri?): String? {
        if (contentUri == null) {
            return null
        }

        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = mHostActivity?.contentResolver?.query(contentUri, proj, null, null, null)
            ?: return ""
        if (cursor.moveToFirst()) {
            val column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    /**
     * 检测sd卡是否可用
     * */
    private fun isSDAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun isSamsungMobile(): Boolean {
        val manufacturer = Build.MANUFACTURER.toLowerCase()
        return (manufacturer.contains("samsung") || manufacturer.contains("lg"))
    }
}