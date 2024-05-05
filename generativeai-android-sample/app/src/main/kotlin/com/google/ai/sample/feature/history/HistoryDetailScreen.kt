package com.google.ai.sample.feature.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.ai.sample.R


data class QuestionItem(
    val question: String,
    val answer: String,
    val score: Int
)

@Preview
@Composable
fun HistoryDetailRoute() {

    val menuItems = listOf(
//        MenuItem("summarize", R.string.menu_summarize_title, R.string.menu_summarize_description),
//        MenuItem("photo_reasoning", R.string.menu_reason_title, R.string.menu_reason_description),
        QuestionItem("请分享一个你在快手实习期间遇到的最难解决的问题", "", 8),
        QuestionItem(
            "请简述你对Android系统架构的理解，特别是在进程管理或虚拟机方面的工作机制。你是如何应用这些知识在你的日常开发工作中的？",
            "",
            7
        ),
        QuestionItem("你可以分享一次你遇到性能问题的场景吗？你是如何分析并优化这个问题的？", "", 5),
        QuestionItem("你能介绍一下MVVM架构吗", "", 5),
        QuestionItem("", "", 5),
        QuestionItem("", "", 5),
        QuestionItem("", "", 5),
    )
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
//        SearchBar(Modifier,searchQuery) { searchQuery = it }
//        LazyColumn {
//            items(filteredMessages) { message ->
//                Text(text = message)
//            }
//        }
        Text(text = "本轮面试整体评价：", fontWeight = FontWeight.Bold)
        Text(
            text =
            "具体优点：" +
                    "技术实力雄厚：你对于Android开发中的关键技术和框架，如Java/Kotlin、Android Studio、Gradle、MVC/MVVM架构等都有深入的了解和实践经验。你的技术知识储备非常扎实。\n" +
                    "项目经验丰富：你分享了过去参与的项目经验，展示了你在实际开发中解决问题的能力。你能够清晰地描述项目中的技术挑战和解决方案。\n" +
                    "具体缺点：对性能优化的深入了解不足：虽然你提到了在项目中关注性能问题，但在面试中对于具体的性能优化技术和策略的描述略显浅薄。对于Android开发工程师来说，深入了解内存管理、渲染优化、线程和并发处理等方面的性能优化技术至关重要。对Android系统底层机制的理解不够深入：你在描述Android开发时，更多地关注于上层框架和API的使用，但对于Android系统底层机制，如虚拟机、进程管理、Binder机制等方面的理解显得不够深入。",
        )

        Text(text = "本轮面试问题：", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
        LazyColumn(
            Modifier
                .padding(bottom = 16.dp)
        ) {
            items(menuItems) { menuItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = menuItem.question,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row (                        modifier = Modifier
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween){
                            Image(
                                painter = painterResource(id = R.mipmap.llm),
                                contentDescription = null,

                                modifier = Modifier.size(20.dp).align(Alignment.CenterVertically)
                            )

                            Text(
                                text = "分数 " + menuItem.score,

                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
