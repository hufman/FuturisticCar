package me.hufman.idriveconnectaddons.futuristiccar

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.bmw.idrive.BMWRemoting
import de.bmw.idrive.BMWRemotingServer
import de.bmw.idrive.BaseBMWRemotingClient
import io.bimmergestalt.idriveconnectkit.CDSProperty
import io.bimmergestalt.idriveconnectkit.IDriveConnection
import io.bimmergestalt.idriveconnectkit.android.CarAppResources
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionStatus
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import me.hufman.idriveconnectaddons.futuristiccar.lib.AndroidResources
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsDouble
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsInt
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsJsonObject
import me.hufman.idriveconnectaddons.futuristiccar.lib.GsonNullable.tryAsJsonPrimitive

const val TAG = "FuturisticCar"

class CarApp(val iDriveConnectionStatus: IDriveConnectionStatus, securityAccess: SecurityAccess,
             val carAppResources: CarAppResources, val androidResources: AndroidResources,
             val carState: CarState, val soundEffectController: SoundController
) {
    val carConnection: BMWRemotingServer
    var cdsHandle = -1
    var amHandle = -1

    private var interestingCds = listOf(
        CDSProperty.DRIVING_KEYPOSITION,
        CDSProperty.DRIVING_ACCELERATORPEDAL,
        CDSProperty.DRIVING_GEAR,
        CDSProperty.DRIVING_SPEEDACTUAL
    )

    init {
        Log.i(TAG, "Starting connecting to car")
        val carappListener = CarAppListener()
        carConnection = IDriveConnection.getEtchConnection(
            iDriveConnectionStatus.host ?: "127.0.0.1",
            iDriveConnectionStatus.port ?: 8003,
            carappListener
        )
        val appCert = carAppResources.getAppCertificate(iDriveConnectionStatus.brand ?: "")?.readBytes()
        val sas_challenge = carConnection.sas_certificate(appCert)
        val sas_response = securityAccess.signChallenge(challenge = sas_challenge)
        carConnection.sas_login(sas_response)

        createAmApp()

        registerCds()
    }

    private fun createAmApp() {
        if (amHandle < 0) {
            val handle = carConnection.am_create("0", "\u0000\u0000\u0000\u0000\u0000\u0002\u0000\u0000".toByteArray())
            carConnection.am_addAppEventHandler(handle, "me.hufman.idriveconnectaddons.futuristiccar")
            amHandle = handle
        }

        val icon = if (soundEffectController.isPlaying) R.raw.ic_playing else R.raw.ic_paused
        val amInfo = mutableMapOf<Int, Any>(
            0 to 145,   // basecore version
            1 to "",  // app name
            2 to androidResources.getRaw(icon), // icon
            3 to "Multimedia",   // section
            4 to true,
            5 to 1500,   // weight
            8 to -1  // mainstateId
        )
        // language translations, dunno which one is which
        for (languageCode in 101..123) {
            amInfo[languageCode] = ""
        }
        carConnection.am_registerApp(amHandle, "me.hufman.idriveconnectaddons.futuristiccar", amInfo)
    }

    private fun registerCds() {
        cdsHandle = carConnection.cds_create()
        for (property in interestingCds) {
            carConnection.cds_addPropertyChangedEventHandler(
                cdsHandle,
                property.propertyName,
                property.ident.toString(),
                5000
            )
            carConnection.cds_getPropertyAsync(cdsHandle,
                property.ident.toString(),
                property.propertyName)
        }
    }

    inner class CarAppListener(): BaseBMWRemotingClient() {
        override fun am_onAppEvent(handle: Int?, ident: String?, appId: String?, event: BMWRemoting.AMEvent?) {
            if (soundEffectController.isPlaying) {
                registerSlowUpdates()
            } else {
                registerFastUpdates()
            }
            soundEffectController.togglePlayback()
            Thread.sleep(200)
            createAmApp()
        }

        override fun cds_onPropertyChangedEvent(
            handle: Int?,
            ident: String?,
            propertyName: String?,
            propertyValue: String?
        ) {
            val value = JsonParser.parseString(propertyValue) as? JsonObject
            if (propertyName == CDSProperty.DRIVING_KEYPOSITION.propertyName) {
                val running = value?.tryAsJsonObject("keyPosition")?.tryAsJsonPrimitive("running")?.tryAsInt
                if (running != null) {
                    carState.running = running == 1
                }
            }
            if (propertyName == CDSProperty.DRIVING_GEAR.propertyName) {
                val gear = value?.tryAsJsonPrimitive("gear")?.tryAsInt
                if (gear != null) {
                    carState.drivingGear = gear != 1 && gear != 3
                }
            }
            if (propertyName == CDSProperty.DRIVING_ACCELERATORPEDAL.propertyName) {
                val position = value?.tryAsJsonObject("acceleratorPedal")?.tryAsJsonPrimitive("position")?.tryAsDouble
                if (position != null) {
                    carState.pedalState = position / 100.0
                }
            }
            if (propertyName == CDSProperty.DRIVING_SPEEDACTUAL.propertyName) {
                val speed = value?.tryAsJsonPrimitive("speedActual")?.tryAsInt
                if (speed != null) {
                    carState.speedometer = speed / 100.0
                }
            }
        }
    }

    private fun registerFastUpdates() {
        for (property in interestingCds) {
            carConnection.cds_addPropertyChangedEventHandler(
                cdsHandle,
                property.propertyName,
                property.ident.toString(),
                100
            )
        }
    }

    private fun registerSlowUpdates() {
        for (property in interestingCds) {
            carConnection.cds_addPropertyChangedEventHandler(
                cdsHandle,
                property.propertyName,
                property.ident.toString(),
                5000
            )
        }
    }

    fun onDestroy() {
        try {
            Log.i(TAG, "Trying to shut down etch connection")
            IDriveConnection.disconnectEtchConnection(carConnection)
        } catch ( e: java.io.IOError) {
        } catch (e: RuntimeException) {}
    }
}