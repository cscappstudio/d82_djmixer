package com.audioeffect.djmixer.music.mixer.core.ads

import com.cscmobi.libraryads.commons.remote.CSCKonfigModel
import com.cscmobi.libraryads.commons.remote.konfig

object RemoteConfigs : CSCKonfigModel {
    val check_organic_user by konfig("check_organic_user", true)
    val inter_all by konfig("inter_all", true)
    val reward_all by konfig("reward_all", true)
    val native_banner by konfig("native_banner", true)
    val enable_ad_inspector by konfig("enable_ad_inspector", false)
}
