CREATE TABLE IF NOT EXISTS Medidas (
    Fecha TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    ID_Sensor INTEGER NOT NULL DEFAULT 1,
    Tipo_Medicion TEXT NOT NULL,
    Valor_Medicion INTEGER NOT NULL,
    PRIMARY KEY (ID_Sensor, Fecha)
);
