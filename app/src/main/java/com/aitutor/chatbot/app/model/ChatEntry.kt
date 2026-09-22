package com.aitutor.chatbot.app.model

data class ChatEntry(
    val id: String,
    val title: String,
    val toolLabel: String,
    val category: ToolCategory,
    val time: String,
    val dateGroup: String
)

val chatHistory = listOf(
    ChatEntry(
        id = "1",
        title = "Solve: 3x + 7 = 22",
        toolLabel = "Math Solver",
        category = ToolCategory.ProblemSolving,
        time = "2:14 PM",
        dateGroup = "Today"
    ),
    ChatEntry(
        id = "2",
        title = "What causes seasons to change?",
        toolLabel = "AI Chatbot",
        category = ToolCategory.Chat,
        time = "11:40 AM",
        dateGroup = "Today"
    ),
    ChatEntry(
        id = "3",
        title = "Fix my paragraph about volcanoes",
        toolLabel = "Grammar Fixer",
        category = ToolCategory.Writing,
        time = "9:05 AM",
        dateGroup = "Today"
    ),
    ChatEntry(
        id = "4",
        title = "Essay on climate change, five paragraphs",
        toolLabel = "Essay Writer",
        category = ToolCategory.Writing,
        time = "6:52 PM",
        dateGroup = "Yesterday"
    ),
    ChatEntry(
        id = "5",
        title = "Explain photosynthesis simply",
        toolLabel = "Concept Explainer",
        category = ToolCategory.StudyAids,
        time = "4:30 PM",
        dateGroup = "Yesterday"
    ),
    ChatEntry(
        id = "6",
        title = "Summarize Chapter 4: Cell Biology",
        toolLabel = "Notes Summarizer",
        category = ToolCategory.StudyAids,
        time = "Mon",
        dateGroup = "This Week"
    ),
    ChatEntry(
        id = "7",
        title = "Rewrite this paragraph for me",
        toolLabel = "Paraphrasing",
        category = ToolCategory.Writing,
        time = "Mon",
        dateGroup = "This Week"
    ),
    ChatEntry(
        id = "8",
        title = "Explain the steps for these homework problems",
        toolLabel = "Homework Solver",
        category = ToolCategory.ProblemSolving,
        time = "Sun",
        dateGroup = "This Week"
    ),
)
