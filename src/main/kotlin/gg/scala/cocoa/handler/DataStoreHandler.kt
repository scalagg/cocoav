package gg.scala.cocoa.handler

import com.mongodb.client.model.Filters
import gg.scala.cocoa.lemon.Grant
import gg.scala.cocoa.lemon.LemonPlayer
import gg.scala.cocoa.lemon.Punishment
import gg.scala.cocoa.lemon.TwoFactorModel
import gg.scala.cocoa.lemon.category.PunishmentCategory
import gg.scala.cocoa.lemon.category.PunishmentCategoryIntensity
import gg.scala.cocoa.lemon.rank.Rank
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.impl.CachedDataStoreStorageLayer
import gg.scala.store.storage.impl.MongoDataStoreStorageLayer
import gg.scala.store.storage.type.DataStoreStorageType
import org.bson.conversions.Bson
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author GrowlyX
 * @since 9/21/2021
 */
object DataStoreHandler
{
    val ranks = mutableMapOf<UUID, Rank>()
    val rankOf = mutableMapOf<UUID, UUID>()

    fun configure()
    {
        DataStoreObjectControllerCache.create<Rank>()
        DataStoreObjectControllerCache.create<TwoFactorModel>()
        DataStoreObjectControllerCache.create<Grant>()
        DataStoreObjectControllerCache.create<Punishment>()
        DataStoreObjectControllerCache.create<LemonPlayer>()

        DataStoreObjectControllerCache.findNotNull<Rank>()
            .loadAll(DataStoreStorageType.MONGO)
            .thenAccept { entries ->
                entries.forEach {
                    ranks[it.value.uuid] = it.value
                }
            }
    }

    private fun fetchPunishments(
        filter: Bson,
        test: ((Punishment) -> Boolean)? = null
    ): CompletableFuture<List<Punishment>>
    {
        val controller = DataStoreObjectControllerCache.findNotNull<Punishment>()

        return controller.useLayerWithReturn<MongoDataStoreStorageLayer<Punishment>, CompletableFuture<List<Punishment>>>(
            DataStoreStorageType.MONGO
        ) {
            return@useLayerWithReturn this.loadAllWithFilter(filter).thenApply {
                val mutableList = mutableListOf<Punishment>()

                it.forEach { entry ->
                    if (test?.invoke(entry.value) ?: true)
                    {
                        mutableList.add(entry.value)
                    }
                }

                return@thenApply mutableList
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

    fun getPermissionGrants(uuid: UUID): List<Grant>
    {
        return DataStoreObjectControllerCache.findNotNull<Grant>()
            .useLayerWithReturn<MongoDataStoreStorageLayer<Grant>, List<Grant>>(
                DataStoreStorageType.MONGO
            ) {
                val grants = loadAllWithFilterSync(
                    Filters.eq("target", uuid.toString())
                )

                return@useLayerWithReturn grants.values.toList()
            }
    }

    fun fetchPunishmentsForTargetOfCategory(
        uuid: UUID,
        category: PunishmentCategory
    ) = fetchPunishments(
        Filters.and(
            Filters.eq("target", uuid.toString()),
            Filters.eq("category", category.name)
        )
    )

    fun fetchPunishmentsForTargetOfCategoryAndActive(
        uuid: UUID,
        category: PunishmentCategory
    ) = fetchPunishments(
        Filters.and(
            Filters.eq("target", uuid.toString()),
            Filters.eq("category", category.name)
        )
    ) {
        it.isActive
    }

    fun fetchPunishmentsByExecutorOfCategory(
        uuid: UUID,
        category: PunishmentCategory
    ) = fetchPunishments(
        Filters.and(
            Filters.eq("addedBy", uuid.toString()),
            Filters.eq("category", category.name)
        )
    )

    fun fetchPunishmentsRemovedByOfCategory(
        uuid: UUID,
        category: PunishmentCategory
    ) = fetchPunishments(
        Filters.and(
            Filters.eq("removedBy", uuid.toString()),
            Filters.eq("category", category.name)
        )
    )

    fun fetchPunishmentsRemovedBy(uuid: UUID) =
        fetchPunishments(
            Filters.eq("removedBy", uuid.toString())
        )

    fun fetchAllPunishmentsForTarget(uuid: UUID) =
        fetchPunishments(
            Filters.eq("target", uuid.toString())
        )

    fun fetchAllPunishmentsByExecutor(uuid: UUID) =
        fetchPunishments(
            Filters.eq("addedBy", uuid.toString())
        )

    private fun fetchPlayerAccountsFiltered(filter: Bson) =
        DataStoreObjectControllerCache
            .findNotNull<LemonPlayer>()
            .useLayerWithReturn<MongoDataStoreStorageLayer<LemonPlayer>, CompletableFuture<Map<UUID, LemonPlayer>>>(
                DataStoreStorageType.MONGO
            ) {
                this.loadAllWithFilter(filter)
            }

    fun fetchAlternateAccountsFor(uuid: UUID): CompletableFuture<List<LemonPlayer>>
    {
        return CompletableFuture
            .supplyAsync {
                findPlayer(uuid)
            }
            .thenComposeAsync { lemonPlayer ->
                if (lemonPlayer == null)
                {
                    return@thenComposeAsync CompletableFuture
                        .completedFuture(
                            null to mapOf<UUID, LemonPlayer>()
                        )
                }

                fetchPlayerAccountsFiltered(
                    Filters.and(
                        Filters.ne("uniqueId", uuid.toString()),
                        Filters.or(
                            Filters.eq("ipAddress", lemonPlayer.ipAddress),
                            Filters.`in`(
                                "pastIpAddresses",
                                *lemonPlayer.pastIpAddresses.toTypedArray()
                            )
                        )
                    )
                ).thenApply { lemonPlayer to it }
            }
            .thenApplyAsync {
                it.first
                    ?: return@thenApplyAsync mutableListOf<LemonPlayer>()

                return@thenApplyAsync it.second.values.toList()
            }
            .exceptionally {
                it.printStackTrace()
                return@exceptionally listOf<LemonPlayer>()
            }
    }

    fun findPlayer(uuid: UUID): LemonPlayer?
    {
        return DataStoreObjectControllerCache
            .findNotNull<LemonPlayer>()
            .useLayerWithReturn<CachedDataStoreStorageLayer<LemonPlayer>, LemonPlayer?>(
                DataStoreStorageType.CACHE
            ) {
                connection.getConnection()[uuid]
            }
    }

    fun findPlayerNullable(uuid: UUID): LemonPlayer?
    {
        return DataStoreObjectControllerCache
            .findNotNull<LemonPlayer>()
            .useLayerWithReturn<CachedDataStoreStorageLayer<LemonPlayer>, LemonPlayer?>(
                DataStoreStorageType.CACHE
            ) {
                connection.getConnection()[uuid]
            }
    }

    fun findRank(uuid: UUID?): Rank?
    {
        return ranks.values
            .firstOrNull { it.uuid == uuid }
    }

    @JvmStatic
    fun getProminentGrant(lemonSpigotGrants: Collection<Grant>): Grant?
    {
        return lemonSpigotGrants
            .sortedByDescending { it.addedAt }
            .sortedByDescending { it.getRank()?.weight ?: 0 }
            .firstOrNull { it.isActive && it.getRank()?.visible ?: false && it.scopes.contains("global") }
    }

    fun findRank(name: String): Rank?
    {
        return ranks.values.firstOrNull {
            it.name.equals(name, true)
        }
    }

    private fun fetchGrants(filter: Bson, test: (Grant) -> Boolean): CompletableFuture<List<Grant>>
    {
        val controller = DataStoreObjectControllerCache.findNotNull<Grant>()

        return controller.useLayerWithReturn<MongoDataStoreStorageLayer<Grant>, CompletableFuture<List<Grant>>>(
            DataStoreStorageType.MONGO
        ) {
            return@useLayerWithReturn this.loadAllWithFilter(filter).thenApply {
                val mutableList = mutableListOf<Grant>()

                it.forEach { entry ->
                    if (test.invoke(entry.value))
                    {
                        mutableList.add(entry.value)
                    }
                }

                return@thenApply mutableList
            }
        }
    }

    fun fetchGrantsFor(uuid: UUID?): CompletableFuture<List<Grant>>
    {
        return fetchGrants(
            Filters.eq("target", uuid.toString())
        ) { true }
    }

    fun getDefaultRank(): Rank
    {
        return findRank("Default")!!
    }
}
