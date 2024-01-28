package presentation.ui.main.comment.view_model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import business.core.DataState
import business.core.NetworkState
import business.core.Queue
import business.core.UIComponent
import business.core.UIComponentState
import business.interactors.main.AddBasketInteractor
import business.interactors.main.AddCommentInteractor
import business.interactors.main.GetCommentsInteractor
import business.interactors.main.HomeInteractor
import business.interactors.main.LikeInteractor
import business.interactors.main.ProductInteractor
import business.interactors.splash.CheckTokenInteractor
import business.interactors.splash.LoginInteractor
import business.interactors.splash.RegisterInteractor
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import presentation.ui.main.detail.view_model.DetailEvent
import presentation.ui.main.home.view_model.HomeEvent

class CommentViewModel(
    private val getCommentsInteractor: GetCommentsInteractor,
    private val addCommentInteractor: AddCommentInteractor,
) : ViewModel() {


    private val TAG = "AppDebug CommentViewModel"


    val state: MutableState<CommentState> = mutableStateOf(CommentState())


    fun onTriggerEvent(event: CommentEvent) {
        when (event) {

            is CommentEvent.AddComment -> {
                addComment(comment = event.comment, rate = event.rate)
            }

            is CommentEvent.OnUpdateAddCommentDialogState -> {
                onUpdateAddCommentDialogState(event.value)
            }

            is CommentEvent.GetComments -> {
                getComments()
            }

            is CommentEvent.OnUpdateProductId -> {
                onUpdateProductId(event.id)
            }

            is CommentEvent.OnRemoveHeadFromQueue -> {
                removeHeadMessage()
            }

            is CommentEvent.Error -> {
                appendToMessageQueue(event.uiComponent)
            }

            is CommentEvent.OnRetryNetwork -> {
                onRetryNetwork()
            }

            is CommentEvent.OnUpdateNetworkState -> {
                onUpdateNetworkState(event.networkState)
            }
        }
    }

    private fun onUpdateProductId(id: Int) {
        state.value = state.value.copy(productId = id)
    }


    private fun addComment(comment: String, rate: Double) {
        addCommentInteractor.execute(
            productId = state.value.productId, rate = rate, comment = comment
        ).onEach { dataState ->
            when (dataState) {
                is DataState.NetworkStatus -> {}
                is DataState.Response -> {
                    onTriggerEvent(CommentEvent.Error(dataState.uiComponent))
                }

                is DataState.Data -> {
                    dataState.data?.let {
                        if (it) onTriggerEvent(CommentEvent.GetComments)
                    }
                }

                is DataState.Loading -> {
                    state.value =
                        state.value.copy(progressBarState = dataState.progressBarState)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun getComments() {
        getCommentsInteractor.execute(state.value.productId).onEach { dataState ->
            when (dataState) {
                is DataState.NetworkStatus -> {}
                is DataState.Response -> {
                    onTriggerEvent(CommentEvent.Error(dataState.uiComponent))
                }

                is DataState.Data -> {
                    dataState.data?.let {
                        state.value = state.value.copy(comments = it)
                    }
                }

                is DataState.Loading -> {
                    state.value =
                        state.value.copy(progressBarState = dataState.progressBarState)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun onUpdateAddCommentDialogState(value: UIComponentState) {
        state.value = state.value.copy(addCommentDialogState = value)
    }

    private fun appendToMessageQueue(uiComponent: UIComponent) {
        if (uiComponent is UIComponent.None) {
            println("${TAG}: onTriggerEvent:  ${(uiComponent as UIComponent.None).message}")
            return
        }

        val queue = state.value.errorQueue
        queue.add(uiComponent)
        state.value = state.value.copy(errorQueue = Queue(mutableListOf())) // force recompose
        state.value = state.value.copy(errorQueue = queue)
    }

    private fun removeHeadMessage() {
        try {
            val queue = state.value.errorQueue
            queue.remove() // can throw exception if empty
            state.value = state.value.copy(errorQueue = Queue(mutableListOf())) // force recompose
            state.value = state.value.copy(errorQueue = queue)
        } catch (e: Exception) {
            println("${TAG}: removeHeadMessage: Nothing to remove from DialogQueue")
        }
    }


    private fun onRetryNetwork() {
        onTriggerEvent(CommentEvent.GetComments)
    }


    private fun onUpdateNetworkState(networkState: NetworkState) {
        state.value = state.value.copy(networkState = networkState)
    }


}