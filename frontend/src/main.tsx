import React from 'react'
import ReactDOM from 'react-dom/client'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import App from './WorkforceApp'

const UserManual = React.lazy(() => import('./UserManual'))
const showManual = new URLSearchParams(window.location.search).get('view') === 'user-manual'

const theme = createTheme({
  palette: { primary: { main: '#2457a6' }, background: { default: '#f4f7fb' } },
  shape: { borderRadius: 10 },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}><CssBaseline />{showManual
      ? <React.Suspense fallback={<p role="status">Loading user manual…</p>}><UserManual /></React.Suspense>
      : <App />}</ThemeProvider>
  </React.StrictMode>,
)
