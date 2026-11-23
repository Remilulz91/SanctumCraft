package fr.sanctumcraft.instance;

/** Role of a room in the sanctum graph. */
public enum RoomKind {
    /** Start room: spawn point + exit portal. Always on the north edge of the grid. */
    ENTRANCE,
    /** Contains an egg nest (gameplay in 0.2). */
    NEST,
    /** Farthest room: destination mechanism / extraction (gameplay in 0.2). */
    DESTINATION,
    /** Filler room. */
    HALL
}
