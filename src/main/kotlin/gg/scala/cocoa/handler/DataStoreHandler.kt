package gg.scala.cocoa.handler

import com.mongodb.client.model.Filters
import com.solexgames.datastore.commons.connection.impl.mongo.UriMongoConnection
import com.solexgames.datastore.commons.layer.impl.MongoStorageLayer
import com.solexgames.datastore.commons.storage.impl.MongoStorageBuilder
import gg.scala.cocoa.CocoaVelocitySettings
import gg.scala.cocoa.lemon.LemonSpigotGrant
import gg.scala.cocoa.lemon.rank.LemonSpigotRank
import gg.scala.cocoa.settings.CocoaSettingsProcessor
import org.bson.conversions.Bson
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object DataStoreHandler {

    val ranks = mutableMapOf<UUID, LemonSpigotRank>()

    lateinit var lemonSpigotRankLayer: MongoStorageLayer<LemonSpigotRank>
    lateinit var lemonSpigotGrantLayer: MongoStorageLayer<LemonSpigotGrant>

    val rankOf = mutableMapOf<UUID, UUID>()

    fun initialLoad()
    {
        val settings = CocoaSettingsProcessor.locate<CocoaVelocitySettings>()!!
        val mongoConnection = UriMongoConnection(
            settings.mongoUri
        )
        val database = "SolexGames"

        this.lemonSpigotRankLayer = MongoStorageBuilder<LemonSpigotRank>()
            .setDatabase(database).setCollection("lemon_ranks")
            .setConnection(mongoConnection).setType(LemonSpigotRank::class.java).build()

        this.lemonSpigotGrantLayer = MongoStorageBuilder<LemonSpigotGrant>()
            .setDatabase(database).setCollection("lemon_grants")
            .setConnection(mongoConnection).setType(LemonSpigotGrant::class.java).build()

        lemonSpigotRankLayer.fetchAllEntries().whenComplete { entries, _ ->
            entries.forEach {
                ranks[it.value.uuid] = it.value
            }
        }
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

    fun loadRankOfNow(uuid: UUID): LemonSpigotRank
    {
        val grants = lemonSpigotGrantLayer.fetchAllEntriesWithFilterSync(
            Filters.eq("target", uuid.toString())
        ) ?: return getDefaultRank()

        val prominent = getProminentGrant(grants.values)

        return if (prominent == null)
        {
            getDefaultRank()
        } else
        {
            prominent.getRank() ?: getDefaultRank()
        }
    }

    fun findRank(uuid: UUID?): LemonSpigotRank? {
        return ranks.values.firstOrNull {
            it.uuid == uuid
        }
    }

    @JvmStatic
    fun getProminentGrant(lemonSpigotGrants: Collection<LemonSpigotGrant>): LemonSpigotGrant? {
        return lemonSpigotGrants
            .sortedByDescending { it.addedAt }
            .sortedByDescending { it.getRank()?.weight ?: 0 }
            .firstOrNull { !it.isRemoved && !it.hasExpired && it.getRank()?.visible ?: false && it.scopes.contains("global") }
    }

    fun findRank(name: String): LemonSpigotRank? {
        return ranks.values.firstOrNull {
            it.name.equals(name, true)
        }
    }

    private fun fetchGrants(filter: Bson, test: (LemonSpigotGrant) -> Boolean): CompletableFuture<List<LemonSpigotGrant>> {
        return lemonSpigotGrantLayer.fetchAllEntriesWithFilter(filter).thenApply {
            val mutableList = mutableListOf<LemonSpigotGrant>()

            it.forEach { entry ->
                if (test.invoke(entry.value)) {
                    mutableList.add(entry.value)
                }
            }

            return@thenApply mutableList
        }
    }

    fun fetchGrantsFor(uuid: UUID?): CompletableFuture<List<LemonSpigotGrant>> {
        return fetchGrants(
            Filters.eq("target", uuid.toString())
        ) { true }
    }

    fun getDefaultRank(): LemonSpigotRank {
        return findRank("Default")!!
    }
}
