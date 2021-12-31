package gg.scala.cocoa.lemon.rank

import gg.scala.cocoa.handler.DataStoreHandler
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

class Rank(
    val uuid: UUID = UUID.randomUUID(),
    var name: String
) : IDataStoreObject {
    override val identifier: UUID
        get() = uuid

    var weight: Int = 0

    var prefix: String = ""
    var suffix: String = ""
    var color: String = ""

    var visible = true

    val children = ArrayList<UUID>()
    val permissions = ArrayList<String>()

    fun getCompoundedPermissions(): ArrayList<String>
    {
        val compoundedPermissions = ArrayList<String>()

        permissions.forEach {
            if (!compoundedPermissions.contains(it))
            {
                compoundedPermissions.add(it)
            }
        }

        children.forEach {
            DataStoreHandler.findRank(it)?.let { rank ->
                rank.getCompoundedPermissions().forEach { permission ->
                    if (!compoundedPermissions.contains(permission))
                    {
                        compoundedPermissions.add(permission)
                    }
                }
            }
        }

        return compoundedPermissions
    }

}
