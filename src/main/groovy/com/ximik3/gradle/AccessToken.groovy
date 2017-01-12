package com.ximik3.gradle

import com.google.gson.annotations.SerializedName

/**
 * Google Drive API v3 token response model
 * Created by volodymyr.kukhar on 10/25/16.
 */
class AccessToken {
    @SerializedName("access_token")
    String accessToken

    @SerializedName("token_type")
    String tokenType

    @SerializedName("expires_in")
    Integer expiresIn
}
