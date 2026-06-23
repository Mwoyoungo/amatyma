package com.lokaleza.amatyma.social

/** A single comment on a post (rendered in the comments sheet). */
data class Comment(
    val id: String,
    val authorUid: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatar: String,
    val text: String,
    val likeCount: Int,
    val timeMillis: Long,
    val replyCount: Int = 0,
)
