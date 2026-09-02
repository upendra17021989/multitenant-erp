import { AppBar, Box, Chip, Container, Paper, Stack, Toolbar, Typography } from '@mui/material'

const milestones = [
  'Tenant-aware platform foundation',
  'Authentication and per-company authorization',
  'Organisation setup and employee master',
  'Attendance, leave, salary and payroll',
]

export default function App() {
  return (
    <Box sx={{ minHeight: '100vh' }}>
      <AppBar position="static" elevation={0}>
        <Toolbar sx={{ gap: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>HR & Payroll ERP</Typography>
          <Chip label="No active company" color="warning" />
        </Toolbar>
      </AppBar>
      <Container maxWidth="md" sx={{ py: 8 }}>
        <Paper sx={{ p: 4 }}>
          <Typography color="primary">MILESTONE 1</Typography>
          <Typography variant="h3" gutterBottom>Platform foundation</Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            The active company will always be visible and authorized by the backend.
          </Typography>
          <Stack spacing={2}>
            {milestones.map((item, index) => <Chip key={item} label={`${index + 1}. ${item}`} />)}
          </Stack>
        </Paper>
      </Container>
    </Box>
  )
}
