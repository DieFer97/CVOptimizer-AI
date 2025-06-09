package com.example.cvoptimizer

import android.os.Parcel
import android.os.Parcelable


data class ApiResponse(
    val success: Boolean,
    val prediccion_principal: String? = null,
    val confianza_principal: Int? = null,
    val todas_las_areas: List<Area>? = null,
    val metadatos: Metadata? = null,
    val archivo_info: FileInfo? = null,
    val error: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        success = parcel.readByte() != 0.toByte(),
        prediccion_principal = parcel.readString(),
        confianza_principal = parcel.readInt().let { if (it == -1) null else it },
        todas_las_areas = parcel.createTypedArrayList(Area.CREATOR),
        metadatos = parcel.readParcelable(Metadata::class.java.classLoader),
        archivo_info = parcel.readParcelable(FileInfo::class.java.classLoader),
        error = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (success) 1 else 0)
        parcel.writeString(prediccion_principal)
        parcel.writeInt(confianza_principal ?: -1)
        parcel.writeTypedList(todas_las_areas)
        parcel.writeParcelable(metadatos, flags)
        parcel.writeParcelable(archivo_info, flags)
        parcel.writeString(error)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ApiResponse> {
        override fun createFromParcel(parcel: Parcel): ApiResponse = ApiResponse(parcel)
        override fun newArray(size: Int): Array<ApiResponse?> = arrayOfNulls(size)
    }
}

data class Area(
    val area: String,
    val porcentaje: Int,
    val confianza: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        area = parcel.readString() ?: "",
        porcentaje = parcel.readInt(),
        confianza = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(area)
        parcel.writeInt(porcentaje)
        parcel.writeString(confianza)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Area> {
        override fun createFromParcel(parcel: Parcel): Area = Area(parcel)
        override fun newArray(size: Int): Array<Area?> = arrayOfNulls(size)
    }
}

data class Metadata(
    val texto_caracteres: Int,
    val timestamp: String,
    val modelo_version: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        texto_caracteres = parcel.readInt(),
        timestamp = parcel.readString() ?: "",
        modelo_version = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(texto_caracteres)
        parcel.writeString(timestamp)
        parcel.writeString(modelo_version)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Metadata> {
        override fun createFromParcel(parcel: Parcel): Metadata = Metadata(parcel)
        override fun newArray(size: Int): Array<Metadata?> = arrayOfNulls(size)
    }
}

data class FileInfo(
    val nombre: String,
    val tamano_bytes: Int,
    val formato: String? = null,
    val caracteres_extraidos: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        nombre = parcel.readString() ?: "",
        tamano_bytes = parcel.readInt(),
        formato = parcel.readString(),
        caracteres_extraidos = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nombre)
        parcel.writeInt(tamano_bytes)
        parcel.writeString(formato)
        parcel.writeInt(caracteres_extraidos)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FileInfo> {
        override fun createFromParcel(parcel: Parcel): FileInfo = FileInfo(parcel)
        override fun newArray(size: Int): Array<FileInfo?> = arrayOfNulls(size)
    }
}
