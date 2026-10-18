import Dashboard from './components/Dashboard'
import BackendGate from './components/BackendGate'

function App() {
  
  return (
    <BackendGate>
      <Dashboard />
    </BackendGate>
  );
}

export default App
