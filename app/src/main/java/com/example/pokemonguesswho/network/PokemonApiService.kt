package com.example.pokemonguesswho.network

import com.example.pokemonguesswho.data.Pokemon
import com.example.pokemonguesswho.data.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokemonApiService {
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 151,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse

    @GET("pokemon/{id}")
    suspend fun getPokemonDetails(@Path("id") id: Int): Pokemon

    @GET("pokemon/{name}")
    suspend fun getPokemonByName(@Path("name") name: String): Pokemon
}
