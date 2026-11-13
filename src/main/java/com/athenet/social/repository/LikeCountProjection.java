package com.athenet.social.repository;

/*
    Proyeccion del conteo agrupado. Spring Data implementa esta interfaz sola a
    partir de los alias del SELECT, asi no hace falta una clase intermedia.
*/
public interface LikeCountProjection {

    String getTargetId();

    long getTotal();
}
