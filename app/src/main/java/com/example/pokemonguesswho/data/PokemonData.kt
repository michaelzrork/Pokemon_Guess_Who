package com.example.pokemonguesswho.data

import com.google.gson.annotations.SerializedName

// Pokemon basic info from PokeAPI
data class Pokemon(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val baseExperience: Int? = null,
    @SerializedName("sprites")
    val sprites: Sprites,
    @SerializedName("types")
    val types: List<TypeSlot>,
    @SerializedName("stats")
    val stats: List<Stat> = emptyList(),
    @SerializedName("abilities")
    val abilities: List<AbilitySlot> = emptyList()
)

data class Sprites(
    @SerializedName("front_default")
    val frontDefault: String? = null,
    @SerializedName("front_shiny")
    val frontShiny: String? = null,
    @SerializedName("other")
    val other: OtherSprites? = null
)

data class OtherSprites(
    @SerializedName("official-artwork")
    val officialArtwork: OfficialArtwork? = null
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String? = null,
    @SerializedName("front_shiny")
    val frontShiny: String? = null
)

data class TypeSlot(
    @SerializedName("type")
    val type: TypeInfo,
    val slot: Int
)

data class TypeInfo(
    val name: String,
    val url: String
)

data class Stat(
    @SerializedName("base_stat")
    val baseStat: Int,
    val effort: Int,
    @SerializedName("stat")
    val stat: StatInfo
)

data class StatInfo(
    val name: String,
    val url: String
)

data class AbilitySlot(
    @SerializedName("ability")
    val ability: AbilityInfo,
    @SerializedName("is_hidden")
    val isHidden: Boolean,
    val slot: Int
)

data class AbilityInfo(
    val name: String,
    val url: String
)

// Local Pokemon game representation
data class GamePokemon(
    val pokemonId: Int,
    val name: String,
    val imageUrl: String,
    val types: List<String>,
    val hp: Int = 0,
    val attack: Int = 0,
    val defense: Int = 0,
    val spAtk: Int = 0,
    val spDef: Int = 0,
    val speed: Int = 0,
    val evolutionStage: String = "Basic",
    var isEliminated: Boolean = false
)

// API response wrapper
data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokemonResult>
)

data class PokemonResult(
    val name: String,
    val url: String
)
