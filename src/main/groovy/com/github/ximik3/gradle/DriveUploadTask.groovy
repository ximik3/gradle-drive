package com.github.ximik3.gradle

import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Android apk uploading task
 * Created by volodymyr.kukhar on 10/24/16.
 */
class DriveUploadTask extends DefaultTask {
    final String APK_MIME_TYPE = 'application/vnd.android.package-archive'

    DrivePluginExtension extension
    ApplicationVariant variant
    DriveUploader uploader

    @TaskAction
    upload() {
        if (uploader == null) {
            uploader = AuthorizationHelper.authorize(extension)
            println 'Token from googleapis.com retrieved'
        }

        variant.outputs
                .findAll() { variantOutput -> variantOutput instanceof ApkVariantOutput }
                .each { variantOutput -> uploader.upload(variantOutput.outputFile, APK_MIME_TYPE, extension.folderId)
        }
    }
}
