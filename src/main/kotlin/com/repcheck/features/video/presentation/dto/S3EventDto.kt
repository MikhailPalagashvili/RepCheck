package com.repcheck.features.video.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class S3EventNotification(
    val Records: List<S3EventRecord>
)

@Serializable
data class S3EventRecord(
    val eventName: String,
    val s3: S3Entity
)

@Serializable
data class S3Entity(
    val bucket: Bucket,
    val `object`: S3Object
)

@Serializable
data class Bucket(
    val name: String
)

@Serializable
data class S3Object(
    val key: String
)
