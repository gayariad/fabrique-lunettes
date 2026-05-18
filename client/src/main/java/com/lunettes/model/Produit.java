package com.lunettes.model;

/**
 * Represente un produit charge depuis {@code products.json}.
 *
 * @param id          identifiant technique du produit (ex: {@code "claude"})
 * @param name        nom affiche dans l'interface
 * @param price       prix en euros
 * @param badge       etiquette optionnelle affichee sur la carte (peut etre {@code null})
 * @param description texte descriptif affiche sous le nom
 */
public record Produit(String id, String name, double price, String badge, String description) {}
