package com.example.mvvmtodoapp.todo_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmtodoapp.data.Todo
import com.example.mvvmtodoapp.data.TodoRepository
import com.example.mvvmtodoapp.util.Routes
import com.example.mvvmtodoapp.util.TodoListEvent
import com.example.mvvmtodoapp.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {
    val todos = repository.getTodos()

    private val _uiEvent = Channel<UiEvent>() // mutable
    val uiEvent = _uiEvent.receiveAsFlow() // immutable

    // For undo purposes.
    private var deletedTodo: Todo? = null

    // On event from the Ui view on user interaction
    fun onEvent(event: TodoListEvent) {
        when (event) {
            is TodoListEvent.OnTodoClick -> {

                sendUiEvent(UiEvent.Navigate(Routes.ADD_EDIT_TODO + "?todo=${event.todo.id}"))
            }
            is TodoListEvent.OnAddTodoClick -> {
                sendUiEvent(UiEvent.Navigate(Routes.ADD_EDIT_TODO))

            }
            is TodoListEvent.OnDoneChange -> {
                viewModelScope.launch {
                    repository.insertTodo(
                        event.todo.copy(
                            isDone = event.isDone
                        )
                    )
                }
            }
            is TodoListEvent.OnUndoDeleteClick -> {
                deletedTodo?.let { todo ->
                    viewModelScope.launch {
                        repository.insertTodo(todo)
                    }
                }

            }
            is TodoListEvent.OnDeleteTodoClick -> {
                viewModelScope.launch {
                    deletedTodo = event.todo
                    repository.deleteTodo(event.todo)
                    sendUiEvent(
                        UiEvent.ShowSnackbar(
                            message = "Todo deleted",
                            action = "Undo"
                        )
                    )
                }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }
}