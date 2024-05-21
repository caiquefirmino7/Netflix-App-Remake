package co.tiagoaguiar.netflixremake.model

import com.caique.netflixappremake.model.Movie

data class MovieDetail(
    val movie: Movie,
    val similars: List<Movie>
)