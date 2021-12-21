package me.hufman.idriveconnectaddons.futuristiccar

import io.bimmergestalt.idriveconnectkit.android.CarAppResources
import io.bimmergestalt.idriveconnectkit.android.IDriveConnectionStatus
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess
import me.hufman.idriveconnectaddons.futuristiccar.lib.AndroidResources

const val TAG = "FuturisticCar"

class CarApp(val iDriveConnectionStatus: IDriveConnectionStatus, securityAccess: SecurityAccess,
             val carAppResources: CarAppResources, val androidResources: AndroidResources
) {
}