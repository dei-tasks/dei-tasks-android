package com.mindmup.android.tasks

case class Node(title: String, id: String, ideas: Option[Map[String, Node]], attr: Option[Attributes])
case class Attributes(style: Map[String, String], progress: String)
