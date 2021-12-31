package gg.scala.cocoa.handler

import com.mongodb.client.model.Filters
import gg.scala.cocoa.lemon.Grant
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.impl.MongoDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import net.evilblock.cubed.serializers.Serializers
import org.bson.conversions.Bson
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object DataStoreHandler {

    val ranks = mutableMapOf<UUID, Rank>()
    val rankOf = mutableMapOf<UUID, UUID>()

    fun initialLoad()
    {
        DataStoreObjectControllerCache.create<Rank>(Serializers.gson)
        DataStoreObjectControllerCache.create<Grant>(Serializers.gson)
    }

    fun updateRank(uuid: UUID)
    {
        fetchGrantsFor(uuid).thenAccept {
            if (it.isNullOrEmpty())
            {
                rankOf[uuid] = getDefaultRank().uuid
            } else
            {
                val prominent = getProminentGrant(it)

                if (prominent != null)
                {
                    rankOf[uuid] = prominent.getRank()?.uuid ?: getDefaultRank().uuid
                } else
                {
                    rankOf[uuid] = getDefaultRank().uuid
                }
            }
        }
    }

    fun loadRankOfNow(uuid: UUID): Rank
    {
        return DataStoreObjectControllerCache.findNotNull<Grant>()
            .useLayerWithReturn<MongoDataStoreStorageLayer<Grant>, Rank>(
                DataStoreStorageType.MONGO
            ) {
                val grants = loadAllWithFilterSync(
                    Filters.eq("target", uuid.toString())
                )
                val prominent = getProminentGrant(grants.values)

                return@useLayerWithReturn if (prominent == null)
                {
                    getDefaultRank()
                } else
                {
                    prominent.getRank() ?: getDefaultRank()
                }
            }
    }

    fun findRank(uuid: UUID?): Rank? {
        return ranks.values.firstOrNull {
            it.uuid == uuid
        }
    }

    @JvmStatic
    fun getProminentGrant(lemonSpigotGrants: Collection<Grant>): Grant? {
        return lemonSpigotGrants
            .sortedByDescending { it.addedAt }
            .sortedByDescending { it.getRank()?.weight ?: 0 }
            .firstOrNull { !it.isRemoved && !it.hasExpired && it.getRank()?.visible ?: false && it.scopes.contains("global") }
    }

    fun findRank(name: String): Rank? {
        return ranks.values.firstOrNull {
            it.name.equals(name, true)
        }
    }

    private fun fetchGrants(filter: Bson, test: (Grant) -> Boolean): CompletableFuture<List<Grant>> {
        val controller = DataStoreObjectControllerCache.findNotNull<Grant>()

        return controller.useLayerWithReturn<MongoDataStoreStorageLayer<Grant>, CompletableFuture<List<Grant>>>(
            DataStoreStorageType.MONGO
        ) {
            return@useLayerWithReturn this.loadAllWithFilter(filter).thenApply {
                val mutableList = mutableListOf<Grant>()

                it.forEach { entry ->
                    if (test.invoke(entry.value)) {
                        mutableList.add(entry.value)
                    }
                }

                return@thenApply mutableList
            }
        }
    }

    fun fetchGrantsFor(uuid: UUID?): CompletableFuture<List<Grant>> {
        return fetchGrants(
            Filters.eq("target", uuid.toString())
        ) { true }
    }

    fun getDefaultRank(): Rank {
        return findRank("Default")!!
    }
}
