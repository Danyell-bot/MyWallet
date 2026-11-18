package com.example.mywallet.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The web app sometimes saved numbers as text (from input boxes). These accept both. */
object LD : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LD", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double {
        val e = (decoder as JsonDecoder).decodeJsonElement()
        return (e as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.0
    }
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

object LDN : KSerializer<Double?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LDN", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double? {
        val e = (decoder as JsonDecoder).decodeJsonElement()
        return (e as? JsonPrimitive)?.content?.toDoubleOrNull()
    }
    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeDouble(0.0) else encoder.encodeDouble(value)
    }
}

object LI : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LI", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int {
        val e = (decoder as JsonDecoder).decodeJsonElement()
        return (e as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 0
    }
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

@Serializable data class Fee(val n: String = "", @Serializable(with = LD::class) val a: Double = 0.0)

@Serializable data class BankCfg(
    val bankName: String = "",
    val rule: String = "avg",
    @Serializable(with = LD::class) val min: Double = 0.0,
    @Serializable(with = LD::class) val fee: Double = 0.0,
    val period: String = "calendar",
    @Serializable(with = LI::class) val startDay: Int = 1,
    @Serializable(with = LD::class) val interest: Double = 0.0,
    val otherFees: List<Fee> = emptyList(),
)

@Serializable data class Account(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val kind: String = "cash",
    @Serializable(with = LD::class) val initial: Double = 0.0,
    val openDate: String = "",
    val ignored: Boolean = false,
    val color: Int? = null,
    @Serializable(with = LD::class) val rate: Double = 1.0,
    val cur: String = "",
    val bank: BankCfg? = null,
)

@Serializable data class Cat(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val type: String = "expense",
    val ignored: Boolean = false,
)

@Serializable data class Rec(
    val id: String = "",
    @Serializable(with = LD::class) val ts: Double = 0.0,
    val type: String = "expense",
    val acct: String = "",
    val to: String = "",
    val cat: String = "",
    @Serializable(with = LD::class) val amount: Double = 0.0,
    @Serializable(with = LDN::class) val toAmount: Double? = null,
    val date: String = "",
    val note: String = "",
)

@Serializable data class Contrib(val date: String = "", @Serializable(with = LD::class) val amount: Double = 0.0)

@Serializable data class Goal(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    @Serializable(with = LD::class) val target: Double = 0.0,
    val color: Int? = null,
    val deadline: String = "",
    val acct: String? = null,
    val contribs: List<Contrib> = emptyList(),
)

@Serializable data class Fav(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val type: String = "expense",
    val cat: String = "",
    val acct: String = "",
    @Serializable(with = LD::class) val amount: Double = 0.0,
)

@Serializable data class Debt(
    val id: String = "",
    val who: String = "",
    @Serializable(with = LD::class) val amount: Double = 0.0,
    val theyOweMe: Boolean = false,
    val date: String = "",
    val due: String = "",
    val note: String = "",
    val paid: Boolean = false,
)

@Serializable data class Bill(
    val id: String = "",
    val name: String = "",
    @Serializable(with = LD::class) val amount: Double = 0.0,
    @Serializable(with = LI::class) val day: Int = 1,
    val cat: String = "",
    val acct: String = "",
    val paid: List<String> = emptyList(),
)

@Serializable data class PayCfg(
    val mode: String = "off",
    @Serializable(with = LI::class) val d1: Int = 1,
    @Serializable(with = LI::class) val d2: Int = 15,
    @Serializable(with = LI::class) val wd: Int = 5,
)

@Serializable data class Settings(
    val cur: String = "₱",
    val pos: String = "start",
    @Serializable(with = LI::class) val dec: Int = 2,
    val theme: String = "auto",
    val hide: Boolean = false,
    val name: String = "",
    val nameAsked: Boolean = false,
    @Serializable(with = LD::class) val holdPct: Double = 0.0,
    @Serializable(with = LD::class) val payday: Double = 0.0,
    val pay: PayCfg? = null,
    val home: kotlinx.serialization.json.JsonObject? = null,
    val alerts: Boolean = true,
)

@Serializable data class AppState(
    val sample: Boolean = false,
    val settings: Settings = Settings(),
    val accounts: List<Account> = emptyList(),
    val cats: List<Cat> = emptyList(),
    val recs: List<Rec> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val favs: List<Fav> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val budgets: Map<String, Map<String, @Serializable(with = LD::class) Double>> = emptyMap(),
    val unlocked: List<String> = emptyList(),
    val recurring: List<JsonObject> = emptyList(),
    val profiles: List<JsonObject> = emptyList(),
    val badgeInit: Boolean = false,
    @Serializable(with = LD::class) val lastBackup: Double = 0.0,
    @Serializable(with = LD::class) val backupSnooze: Double = 0.0,
    val challenge: JsonObject? = null,
    /** not saved: changes whenever the app asks for a redraw (developer mode time travel) */
    @kotlinx.serialization.Transient val rev: Int = 0,
)

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
