import {useEffect} from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import manual from '../../docs/USER_MANUAL.md?raw'
import './user-manual.css'

export default function UserManual() {
  useEffect(() => {
    document.title = 'User Manual | Amar Group ERP'
    if (window.location.hash) {
      document.getElementById(decodeURIComponent(window.location.hash.slice(1)))?.scrollIntoView()
    }
  }, [])

  return <div className="manual-page">
    <header className="manual-toolbar">
      <a href={import.meta.env.BASE_URL}>Back to ERP</a>
      <span>Amar Group ERP · User Manual</span>
      <button type="button" onClick={() => window.print()}>Print / Save PDF</button>
    </header>
    <main className="manual-content" id="manual-top">
      <Markdown remarkPlugins={[remarkGfm]} components={{
        h2: ({children}) => <h2 id={String(children).toLowerCase().replace(/[^a-z0-9\s-]/g, '').replace(/\s+/g, '-')}>{children}</h2>,
        table: ({children}) => <div className="manual-table" tabIndex={0} role="region" aria-label="Reference table"><table>{children}</table></div>,
      }}>{manual}</Markdown>
      <a className="manual-back-top" href="#manual-top">Back to top</a>
    </main>
  </div>
}
