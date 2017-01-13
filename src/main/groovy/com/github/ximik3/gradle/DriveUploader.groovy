package com.github.ximik3.gradle

/**
 * Drive REST API v3 file uploader class
 * Created by volodymyr.kukhar on 10/24/16.
 */
class DriveUploader {

    private AccessToken accessToken

    DriveUploader(AccessToken accessToken) {
        this.accessToken = accessToken
    }

    void upload(File file, String mimeType, String driveFolderId) throws IOException, SecurityException {
        println('Uploading ' + file.name + "...")
        def uploadId = retrieveUploadId(file.name, mimeType, driveFolderId)
        uploadFile(file, mimeType, uploadId)
    }

    String retrieveUploadId(String filename, String mimeType, String folderId) {
        def url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable")
        def drive = url.openConnection() as HttpURLConnection
        drive.setRequestMethod("POST")
        drive.setRequestProperty("Host", "www.googleapis.com")
        drive.setRequestProperty("Authorization", accessToken.tokenType + " " + accessToken.accessToken)
        drive.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        drive.setRequestProperty("X-Upload-Content-Type", mimeType)
        def body = '\n' +
                '{\n' +
                '  "name": "' + filename + '",\n' +
                '  "parents": [ "' + folderId + '" ]\n' +
                '}\n\n'
        drive.setFixedLengthStreamingMode body.bytes.length     // Content Length
        drive.setDoOutput true
        drive.setDoInput true
        drive.outputStream.write body.bytes

        // response header example:
        //   -> Location: [https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&upload_id=AEnB...]
        // get "Location" header
        String loc = drive.getHeaderFields().get("Location")

        // trim '[' and ']' brackets and get URL query
        def query = new URL(loc[1..-2]).query

        // find upload_id value
        String uploadId = ''
        query.split('&').each {
            if (it.split('=')[0] == 'upload_id')
                uploadId = it.split('=')[1]
        }

        return uploadId
    }

    static void uploadFile(File file, String mimeType, String uploadId) {
        def url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&upload_id=" + uploadId)
        def drive = url.openConnection() as HttpURLConnection
        drive.setRequestMethod 'POST'
        drive.setRequestProperty("Content-Type", mimeType)
        drive.setFixedLengthStreamingMode(file.bytes.length)     // Content Length
        drive.setDoOutput true
        drive.setDoInput true
        drive.outputStream.write file.bytes
    }
}
