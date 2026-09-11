import React from 'react'
import ReactDOM from 'react-dom/client'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import App from './WorkforceApp'

const theme = createTheme({
  palette: { primary: { main: '#2457a6' }, background: { default: '#f4f7fb' } },
  shape: { borderRadius: 10 },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}><CssBaseline /><App /></ThemeProvider>
  </React.StrictMode>,
)
