package com.google.ai.sample.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.ai.sample.R

@Preview
@Composable
fun HistoryRoute(){
    val navController = rememberNavController()
    // 设置 NavHost，管理导航内容
    NavHost(navController = navController, startDestination = "history") {
        composable("historyDetail") { // 首页路由
            HistoryDetailRoute()
        }
        composable("history") { // 首页路由
            HistoryScreen(navController)
        }
    }
}
data class MenuItem(
    val date: String,
    val jd: String,
    val score: Int
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavHostController) {

    val messages = listOf(
        "Message 1",
        "Message 2",
        "Message 3 with more text",
        // ... other messages
    )

    var filteredMessages by remember {
        mutableStateOf(messages)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }
    val menuItems = listOf(
//        MenuItem("summarize", R.string.menu_summarize_title, R.string.menu_summarize_description),
//        MenuItem("photo_reasoning", R.string.menu_reason_title, R.string.menu_reason_description),
        MenuItem("2024年4月25日 17:22", "初级JAVA开发\n" +
                "  1.   公办全日制本科，双证齐全，学信网可查，毕业后工作三年或以上。\n" +
                "  2.   熟悉linux操作系统基础命令\n" +
                "  3.   熟悉Oracle和Mysql数据库，SQL语法熟练\n" +
                "  4.   精通Springboot，Spring Cloud。\n" +
                "  5.   至少使用过一种消息队列，并对其原理有所了解\n" +
                "  6.   JAVA语言基础过关，熟练使用JDK8的Lambda语法\n" +
                "  7.   熟练使用maven和git", 6),
        MenuItem("2024年4月22日 14:10", "android高级工程师\n一、工作职责：\n" +
                "1.负责android手机的App开发工作， 参与产品的设计和改进;\n" +
                "2.负责Android程序架构、性能等方面的优化，解决相关的疑难问题。\n" +
                "二、任职要求：\n" +
                "1.计算机相关专业本科及以上学历，具备扎实的计算机理论基础;\n" +
                "2.3年以上Java开发经验或者Android开发经验，具备扎实的java编程基础;\n" +
                "3.熟悉Android SDK、对Android应用结构有深刻的认识，具备出色的调试知识、经验和技能;\n" +
                "4.熟悉面向对象设计和分析，能够运用常用的设计模式;熟练掌握网络及多线程开发;\n" +
                "5.有强烈的责任心和团队精神，善于沟通和合作，能独立完成设计和编码;\n" +
                "6.有主导完成优秀应用或相关产品开发经验者优先。", 7),
    )

    LaunchedEffect(searchQuery) {
        filteredMessages = messages.filter { it.contains(searchQuery, ignoreCase = true) }
    }
    Column(
            modifier = androidx.compose.ui.Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            SearchBar(Modifier,searchQuery) { searchQuery = it }
//        LazyColumn {
//            items(filteredMessages) { message ->
//                Text(text = message)
//            }
//        }
        LazyColumn(
            Modifier
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            items(menuItems) { menuItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    onClick = {
                        navController.navigate("historyDetail")
                    }
                    ) {
                    Column(
                        modifier = Modifier
                            .padding(all = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = menuItem.date,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = menuItem.jd,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "平均分数 "+menuItem.score,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
//                    TextButton(
//                        onClick = {
//                            onItemClicked(menuItem.routeId)
//                        },
//                        modifier = Modifier.align(Alignment.End)
//                    ) {
//                        Text(text = stringResource(R.string.action_try))
//                    }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChanged: (String) -> Unit
) {
    var value by remember {
        mutableStateOf("")
    }
    // Implement composable here
    TextField(value = query, singleLine = true, onValueChange = { onQueryChanged(it) }, modifier = modifier
        .heightIn(56.dp)
        .fillMaxWidth()
        .padding(horizontal = 0.dp),
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        }, placeholder = { Text(text = stringResource(id = R.string.placeholder_search), textAlign = TextAlign.Center) },
        colors = TextFieldDefaults.textFieldColors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ), shape = RoundedCornerShape(12.dp)
    )
}
