export default function UserManualLink() {
  return <a className="wf-manual-link" href={`${import.meta.env.BASE_URL}?view=user-manual`} target="_blank" rel="noopener noreferrer">User Manual<span className="wf-sr-only"> (opens in a new tab)</span></a>
}
