package org.mozilla.rocket.abtesting

import android.content.Context
import android.preference.PreferenceManager
import androidx.collection.ArrayMap
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.getJsonArray

object LocalAbTesting {
    private val NUMBER_RANGE = (1..20)

    // Current AB Testings
    // Content Hub AB Testing
    const val CONTENT_HUB_AB_TESTING = "content_hub_ab_testing"
    // Smart Shopping Copy AB Testing
    const val SMART_SHOPPING_COPY_AB_TESTING = "smart_shopping_copy_ab_testing"
    // Testing Groups
    const val CONTENT_POSITION_TOPSITE = "content_position_topsite"
    const val SMART_SHOPPING_COPY_B = "smart_shopping_copy_b"

    private lateinit var appContext: Context
    private var isNewGroupAssigned = false
    private val isNewUser: Boolean by lazy {
        userGroup // force to trigger loadUserGroup() if it didn't get called
        isNewGroupAssigned
    }
    private val activeExperiments: List<Experiment> by lazy {
        AssetsUtils.loadStringFromRawResource(appContext, R.raw.abtesting)!!
                .getJsonArray { it.toExperiment() }
                .filter { it.enabled && it.matchNewUserCondition(isNewUser) }
                .also { updateActiveExperiments(it) }
    }
    private val assignedBucketMap = ArrayMap<String, String?>()

    val isActive: Boolean by lazy { activeExperiments.isNotEmpty() }
    val userGroup: Int by lazy { loadUserGroup() }
    val assignedBuckets: List<String> by lazy {
        activeExperiments.flatMap { it.buckets }
                .filter { it.bucket_range_start <= userGroup && userGroup <= it.bucket_range_end }
                .map { it.experiment_name }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun checkAssignedBucket(experiment: String): String? {
        return if (assignedBucketMap.containsKey(experiment)) {
            assignedBucketMap[experiment]
        } else {
            val bucket = activeExperiments.find { experiment == it.name }
                    ?.buckets
                    ?.find { it.bucket_range_start <= userGroup && userGroup <= it.bucket_range_end }
                    ?.experiment_name
            assignedBucketMap[experiment] = bucket
            return bucket
        }
    }

    private fun loadUserGroup(): Int {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext)
        val prefKey = appContext.getString(R.string.pref_key_experiment_bucket)

        var userGroup = sharedPref.getInt(prefKey, -1)
        if (userGroup < 0) {
            isNewGroupAssigned = true
            userGroup = NUMBER_RANGE.random()
            sharedPref.edit().putInt(prefKey, userGroup).apply()
        } else {
            isNewGroupAssigned = false
        }

        return userGroup
    }

    private fun Experiment.matchNewUserCondition(isNewUser: Boolean?): Boolean =
            (!this.newUserOnly || this.newUserOnly && isNewUser == true) || isActiveExperiment(name)

    private fun isActiveExperiment(name: String): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext)
        val prefKey = appContext.getString(R.string.pref_key_is_under_experiment)
        return sharedPref.getString(prefKey, "")
                ?.split(",")
                ?.find { it == name } != null
    }

    private fun updateActiveExperiments(experiments: List<Experiment>) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext)
        val prefKey = appContext.getString(R.string.pref_key_is_under_experiment)
        val value = experiments.joinToString(separator = ",") { it.name }
        sharedPref.edit().putString(prefKey, value).apply()
    }

    private data class Experiment(
        val name: String,
        val enabled: Boolean,
        val newUserOnly: Boolean,
        val buckets: List<Bucket>
    )

    private data class Bucket(
        val experiment_name: String,
        val bucket_range_start: Int,
        val bucket_range_end: Int
    )

    private fun JSONObject.toExperiment() = Experiment(
        name = getString("name"),
        enabled = getBoolean("enabled"),
        newUserOnly = getBoolean("newUserOnly"),
        buckets = getJsonArray("buckets") { it.toBucket() }
    )

    private fun JSONObject.toBucket() = Bucket(
        experiment_name = getString("experiment_name"),
        bucket_range_start = getInt("bucket_range_start"),
        bucket_range_end = getInt("bucket_range_end")
    )
}