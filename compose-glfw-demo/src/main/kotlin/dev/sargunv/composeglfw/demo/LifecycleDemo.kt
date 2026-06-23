package dev.sargunv.composeglfw.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

@Composable
internal fun LifecycleCard(modifier: Modifier = Modifier) {
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val model = viewModel<LifecycleDemoViewModel>(factory = LifecycleDemoViewModel.Factory)

  Card(modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Lifecycle", style = MaterialTheme.typography.titleMedium)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text("State", style = MaterialTheme.typography.labelLarge)
        Text(lifecycleState.name, style = MaterialTheme.typography.bodyMedium)
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(onClick = model::increment) {
          Text("ViewModel ${model.count}")
        }
        Text("Instance ${model.instanceId}", style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

internal class LifecycleDemoViewModel : ViewModel() {
  val instanceId: Int = nextInstanceId()
  var count: Int by mutableIntStateOf(0)
    private set

  fun increment(): Unit {
    count++
  }

  internal companion object {
    private var nextId: Int = 1

    internal val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        LifecycleDemoViewModel()
      }
    }

    fun nextInstanceId(): Int = nextId++
  }
}
