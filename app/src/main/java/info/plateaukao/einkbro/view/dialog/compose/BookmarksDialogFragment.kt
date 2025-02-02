package info.plateaukao.einkbro.view.dialog.compose

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import info.plateaukao.einkbro.R
import info.plateaukao.einkbro.databinding.DialogMenuContextListBinding
import info.plateaukao.einkbro.database.Bookmark
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.NinjaToast
import info.plateaukao.einkbro.view.compose.MyTheme
import info.plateaukao.einkbro.view.compose.NormalTextModifier
import info.plateaukao.einkbro.view.dialog.BookmarkEditDialog
import info.plateaukao.einkbro.view.dialog.DialogManager
import info.plateaukao.einkbro.view.dialog.dismissWithAction
import info.plateaukao.einkbro.viewmodel.BookmarkViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class BookmarksDialogFragment(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val bookmarkViewModel: BookmarkViewModel,
    private val gotoUrlAction: (String) -> Unit,
    private val addTabAction: (String, String, Boolean) -> Unit,
    private val splitScreenAction: (String) -> Unit,
): ComposeDialogFragment(), KoinComponent {
    private val bookmarkManager: BookmarkManager by inject()
    private val dialogManager: DialogManager by lazy { DialogManager(requireActivity()) }

    override fun setupComposeView() = updateBookmarksContent()

    override fun onDestroy() {
        updateContentJob?.cancel()
        super.onDestroy()
    }

    private var updateContentJob: Job? = null
    private val folderStack: Stack<Bookmark> = Stack()
    private fun updateBookmarksContent() {
        if (folderStack.isEmpty()) folderStack.push(Bookmark(getString(R.string.bookmarks), ""))

        val currentFolder = folderStack.peek()
        updateContentJob?.cancel()
        updateContentJob = lifecycleScope.launch {
            bookmarkViewModel.bookmarksByParent(currentFolder.id).collect { bookmarks ->
                composeView.setContent {
                    MyTheme {
                        DialogPanel(
                            folder = currentFolder,
                            upParentAction = { gotoParentFolder() },
                            createFolderAction = { createBookmarkFolder(it) },
                            closeAction = { dialog?.dismiss() }) {
                            if (bookmarks.isEmpty()) {
                                Text(modifier = NormalTextModifier, text = getString(R.string.no_bookmarks), color = MaterialTheme.colors.onBackground)
                            } else {
                                BookmarkList(
                                    bookmarks = bookmarks,
                                    bookmarkManager = bookmarkManager,
                                    isWideLayout = ViewUnit.isWideLayout(requireContext()),
                                    shouldReverse = !config.isToolbarOnTop,
                                    onBookmarkClick = {
                                        if (!it.isDirectory) {
                                            gotoUrlAction(it.url)
                                            config.addRecentBookmark(it)
                                            dialog?.dismiss()
                                        } else {
                                            folderStack.push(it)
                                            updateBookmarksContent()
                                        }
                                    },
                                    onBookmarkIconClick = { if (!it.isDirectory) addTabAction(it.title, it.url, true); dialog?.dismiss() },
                                    onBookmarkLongClick = { showBookmarkContextMenu(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun gotoParentFolder() {
        if (folderStack.size > 1) {
            folderStack.pop()
            updateBookmarksContent()
        }
    }

    private fun createBookmarkFolder(bookmark: Bookmark) {
        lifecycleScope.launch {
            val folderName = dialogManager.getBookmarkFolderName()
            folderName?.let { bookmarkManager.insertDirectory(it, bookmark.id) }
        }
    }

    private fun showBookmarkContextMenu(bookmark: Bookmark) {
        val dialogView = DialogMenuContextListBinding.inflate(LayoutInflater.from(requireContext()))
        val optionDialog = dialogManager.showOptionDialog(dialogView.root)

        if (bookmark.isDirectory) {
            dialogView.menuContextListNewTab.visibility = View.GONE
            dialogView.menuContextListNewTabOpen.visibility = View.GONE
            dialogView.menuContextListSplitScreen.visibility = View.GONE
        }

        dialogView.menuContextListSplitScreen.setOnClickListener {
            optionDialog.dismissWithAction { splitScreenAction(bookmark.url) ; dialog?.dismiss() }
        }

        dialogView.menuContextListEdit.visibility = View.VISIBLE
        dialogView.menuContextListNewTab.setOnClickListener {
            optionDialog.dismissWithAction {
                addTabAction(getString(R.string.app_name), bookmark.url, false)
                NinjaToast.show(context, getString(R.string.toast_new_tab_successful))
                dialog?.dismiss()
            }
        }
        dialogView.menuContextListNewTabOpen.setOnClickListener {
            optionDialog.dismissWithAction { addTabAction(bookmark.title, bookmark.url, true) ; dialog?.dismiss() }
        }
        dialogView.menuContextListDelete.setOnClickListener {
            lifecycleScope.launch { bookmarkManager.delete(bookmark) }
            optionDialog.dismiss()
        }

        dialogView.menuContextListEdit.setOnClickListener {
            BookmarkEditDialog(
                requireActivity(),
                bookmarkManager,
                bookmark,
                { ViewUnit.hideKeyboard(requireActivity()) },
                { ViewUnit.hideKeyboard(requireActivity()) }
            ).show()
            optionDialog.dismiss()
        }
    }
}

@Composable
fun DialogPanel(
    folder: Bookmark,
    upParentAction: (Bookmark)->Unit,
    createFolderAction: (Bookmark)->Unit,
    closeAction: ()->Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Box(Modifier.weight(1F, fill = false)) {
            content()
        }
        Divider(thickness = 1.dp, color = MaterialTheme.colors.onBackground)
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            if (folder.id != 0) {
                ActionIcon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    iconResId = R.drawable.icon_arrow_left_gest,
                    action =  { upParentAction(folder) }
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Text(
                folder.title,
                Modifier
                    .weight(1F)
                    .padding(horizontal = 5.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { if (folder.id != 0) upParentAction(folder) },
                color = MaterialTheme.colors.onBackground
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.ic_add_folder,
                action =  { createFolderAction(folder) }
            )
            ActionIcon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(horizontal = 5.dp),
                iconResId = R.drawable.icon_arrow_down_gest,
                action =  closeAction
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkList(
    bookmarks: List<Bookmark>,
    bookmarkManager: BookmarkManager? = null,
    isWideLayout: Boolean = false,
    shouldReverse: Boolean = true,
    onBookmarkClick: OnBookmarkClick,
    onBookmarkIconClick: OnBookmarkIconClick,
    onBookmarkLongClick: OnBookmarkLongClick,
) {
    LazyVerticalGrid(
        modifier = Modifier.wrapContentHeight(),
        columns = GridCells.Fixed(if (isWideLayout) 2 else 1),
        reverseLayout = shouldReverse
    ){
        itemsIndexed(bookmarks) { _, bookmark ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            BookmarkItem(
                bookmark = bookmark,
                bitmap =  bookmarkManager?.findFaviconBy(bookmark.url)?.getBitmap(),
                isPressed = isPressed,
                modifier = Modifier
                    .combinedClickable (
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onBookmarkClick(bookmark) },
                        onLongClick = { onBookmarkLongClick(bookmark) },
                    ),
                iconClick = {
                    if (!bookmark.isDirectory) onBookmarkIconClick(bookmark)
                    else onBookmarkClick(bookmark)
                }
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    modifier: Modifier,
    bitmap: Bitmap? = null,
    isPressed: Boolean = false,
    bookmark: Bookmark,
    iconClick: ()->Unit,
) {
    val borderWidth = if (isPressed) 1.dp else -1.dp

    Row(
        modifier = modifier
            .height(54.dp)
            .padding(8.dp)
            .border(borderWidth, MaterialTheme.colors.onBackground, RoundedCornerShape(7.dp)),
        horizontalArrangement = Arrangement.Center
    ) {
        if (bitmap!= null) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(36.dp)
                    .padding(end = 5.dp)
                    .clickable { iconClick() },
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
            )
        } else {
            ActionIcon(
                modifier = Modifier.align(Alignment.CenterVertically),
                iconResId = if (bookmark.isDirectory) R.drawable.ic_folder else R.drawable.icon_plus,
                action =  iconClick
            )
        }
        Text(
            modifier = Modifier
                .weight(1F)
                .align(Alignment.CenterVertically),
            text = bookmark.title,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onBackground,
        )
    }
}

@Composable
fun ActionIcon(modifier: Modifier, iconResId: Int, action: (()->Unit)? = null) {
    Icon(
        modifier = modifier
            .size(36.dp)
            .padding(end = 5.dp)
            .clickable { action?.invoke() },
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colors.onBackground
    )
}

@Preview
@Composable
private fun PreviewBookmarkList() {
    MyTheme {
        BookmarkList(
            bookmarks = listOf(Bookmark("test 1","https://www.google.com", false)),
            null,
            isWideLayout = true,
            shouldReverse = true,
            {},
            {},
            {}
        )
    }
}

typealias OnBookmarkClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkLongClick = (bookmark: Bookmark) -> Unit
typealias OnBookmarkIconClick = (bookmark: Bookmark) -> Unit
