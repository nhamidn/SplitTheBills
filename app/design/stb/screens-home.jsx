// Home, Groups, Group detail

function useGroupStats(app) {
  return React.useMemo(() => {
    const out = {};
    app.groups.forEach((g) => {
      const net = stbNet(g.id, app.expenses, app.settlements);
      const ex = app.expenses.filter((e) => e.group === g.id);
      const total = ex.reduce((s, e) => s + e.amount, 0);
      const debts = stbSimplify(net);
      const outstanding = debts.reduce((s, d) => s + d.amount, 0);
      const settled = app.settlements.filter((s) => s.group === g.id && s.status === 'completed').reduce((s, x) => s + x.amount, 0);
      const last = ex.map((e) => e.date).sort().pop();
      out[g.id] = { net, me: net.me || 0, total: r2(total), debts, outstanding: r2(outstanding), settled, count: ex.length, last,
        progress: settled + outstanding > 0 ? settled / (settled + outstanding) : 1 };
    });
    return out;
  }, [app.expenses, app.settlements, app.groups]);
}

function useTotals(app, stats) {
  let owed = 0, owe = 0;
  Object.values(stats).forEach((s) => { if (s.me > 0) owed += s.me; else owe -= s.me; });
  return { owed: r2(owed), owe: r2(owe), net: r2(owed - owe) };
}

function balanceLine(v) {
  if (v > 0.004) return { text: "you're owed", color: T.pos };
  if (v < -0.004) return { text: 'you owe', color: T.neg };
  return { text: 'settled up', color: T.ink3 };
}

// ── HOME ──────────────────────────────────────────────────
function HomeScreen({ app }) {
  const stats = useGroupStats(app);
  const tot = useTotals(app, stats);
  const recent = [...app.expenses].sort((a, b) => b.date.localeCompare(a.date) || b.id.localeCompare(a.id)).slice(0, 4);
  const active = [...app.groups].sort((a, b) => (stats[b.id].last || '').localeCompare(stats[a.id].last || ''));

  return (
    <>
      <ScreenBody>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 12px 8px 20px' }}>
          <Avatar id="me" size={40} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, color: T.ink3, fontWeight: 600 }}>Good morning</div>
            <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.3 }}>Maya</div>
          </div>
          <IconBtn name="search" />
          <IconBtn name="notifications" badge />
        </div>

        {/* Balance hero */}
        <div style={{ padding: '8px 16px 0' }}>
          <div className="hero" style={{ background: T.ink, color: '#fff', borderRadius: 28, padding: '22px 22px 20px', position: 'relative', overflow: 'hidden' }}>
            <div style={{ position: 'absolute', right: -60, top: -70, width: 220, height: 220, borderRadius: '50%', background: 'radial-gradient(circle, oklch(0.56 0.12 160 / 0.35), transparent 70%)' }} />
            <div style={{ fontSize: 13, fontWeight: 600, color: 'rgba(255,255,255,0.6)', position: 'relative' }}>Total balance</div>
            <div className="num" style={{ fontSize: 44, fontWeight: 800, letterSpacing: -1.5, marginTop: 4, position: 'relative', color: tot.net >= 0 ? '#fff' : T.negOnDark }}>
              {stbFmt(tot.net, true)}
            </div>
            <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.6)', fontWeight: 500, marginBottom: 18 }}>
              {tot.net >= 0 ? 'Overall, friends owe you' : 'Overall, you owe friends'}
            </div>
            <SplitBar a={tot.owed} b={tot.owe} ca={T.posOnDark} cb={T.negOnDark} />
            <div style={{ display: 'flex', marginTop: 16, gap: 12 }}>
              {[['You are owed', tot.owed, T.posOnDark, 'south_west'], ['You owe', tot.owe, T.negOnDark, 'north_east']].map(([l, v, c, ic]) => (
                <div key={l} style={{ flex: 1, background: 'rgba(255,255,255,0.07)', borderRadius: 18, padding: '12px 14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12.5, color: 'rgba(255,255,255,0.65)', fontWeight: 600 }}>
                    <span style={{ width: 8, height: 8, borderRadius: 4, background: c }} />{l}
                  </div>
                  <div className="num" style={{ fontSize: 20, fontWeight: 800, marginTop: 4, color: c }}>{stbFmt(v)}</div>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
              <button className="btn-light" onClick={() => app.push({ name: 'balances' })}><Icon name="handshake" size={20} />Settle up</button>
              <button className="btn-ghost-dark" onClick={() => app.toast('Reminders sent to 3 friends')}><Icon name="notifications_active" size={20} />Remind</button>
            </div>
          </div>
        </div>

        {/* Active groups */}
        <div style={{ padding: '28px 16px 0' }}>
          <SectionTitle action="See all" onAction={() => app.tab('groups')}>Active groups</SectionTitle>
        </div>
        <div className="hscroll" style={{ display: 'flex', gap: 12, padding: '0 16px 4px', overflowX: 'auto' }}>
          {active.map((g) => {
            const s = stats[g.id]; const b = balanceLine(s.me);
            return (
              <Card key={g.id} onClick={() => app.push({ name: 'group', id: g.id })} style={{ minWidth: 168, flexShrink: 0 }} pad={16}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <GroupTile group={g} size={44} radius={14} />
                  <AvatarStack ids={g.members} size={22} max={3} />
                </div>
                <div style={{ fontSize: 15, fontWeight: 700, marginTop: 14 }}>{g.name}</div>
                <div style={{ fontSize: 12.5, color: b.color, fontWeight: 600, marginTop: 2 }}>{b.text}</div>
                <div className="num" style={{ fontSize: 18, fontWeight: 800, color: b.color, marginTop: 1 }}>{stbFmt(s.me)}</div>
              </Card>
            );
          })}
        </div>

        {/* Recent */}
        <div style={{ padding: '28px 16px 0' }}>
          <SectionTitle action="View all" onAction={() => app.tab('activity')}>Recent transactions</SectionTitle>
          <Card pad={6}>
            {recent.map((e, i) => <ExpenseRow key={e.id} e={e} app={app} divider={i > 0} showGroup />)}
          </Card>
        </div>
      </ScreenBody>
      <Fab onClick={() => app.push({ name: 'add' })} />
    </>
  );
}

function ExpenseRow({ e, app, divider, showGroup, showAvatars }) {
  const mine = stbMyShare(e);
  const g = app.groups.find((x) => x.id === e.group);
  const payer = e.paidBy === 'me' ? 'You' : STB_PEOPLE[e.paidBy].short;
  return (
    <div className="row press" onClick={() => app.push({ name: 'group', id: e.group })} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 10px', borderTop: divider ? `1px solid ${T.line}` : 0, margin: divider ? '0 4px' : '0 4px' }}>
      <CatTile cat={e.cat} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{e.title}</div>
        <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500, marginTop: 2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {payer} paid <span className="num">{stbFmt(e.amount)}</span>{showGroup && g ? ` · ${g.name}` : ` · ${stbShortDate(e.date)}`}
        </div>
        {showAvatars && <div style={{ marginTop: 8 }}><AvatarStack ids={Object.keys(e.shares)} size={20} max={5} /></div>}
      </div>
      <div style={{ textAlign: 'right', flexShrink: 0 }}>
        <div style={{ fontSize: 11.5, fontWeight: 600, color: mine.kind === 'lent' ? T.pos : mine.kind === 'owe' ? T.neg : T.ink3 }}>
          {mine.kind === 'lent' ? 'you lent' : mine.kind === 'owe' ? 'you owe' : 'not involved'}
        </div>
        {mine.kind !== 'none' && <Money v={mine.value} size={15} color={mine.kind === 'lent' ? T.pos : T.neg} />}
      </div>
    </div>
  );
}

// ── GROUPS ────────────────────────────────────────────────
function GroupsScreen({ app }) {
  const stats = useGroupStats(app);
  const [filter, setFilter] = React.useState('All');
  const kinds = ['All', 'Trip', 'Home', 'Friends', 'Couple'];
  const list = app.groups.filter((g) => filter === 'All' || g.kind === filter);
  return (
    <>
      <ScreenBody>
        <TopBar title="Groups" right={<><IconBtn name="search" /><IconBtn name="more_vert" /></>} />
        <div className="hscroll" style={{ display: 'flex', gap: 8, padding: '4px 16px 16px', overflowX: 'auto' }}>
          {kinds.map((k) => <Chip key={k} active={k === filter} onClick={() => setFilter(k)}>{k === 'All' ? `All · ${app.groups.length}` : k}</Chip>)}
        </div>
        <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 12 }}>
          {list.map((g) => {
            const s = stats[g.id]; const b = balanceLine(s.me);
            return (
              <Card key={g.id} onClick={() => app.push({ name: 'group', id: g.id })} pad={18}>
                <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
                  <GroupTile group={g} size={52} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 16, fontWeight: 800, letterSpacing: -0.2 }}>{g.name}</div>
                    <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500, marginTop: 2 }}>{g.kind} · {g.members.length} members · {g.dates}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 11.5, fontWeight: 600, color: b.color }}>{b.text}</div>
                    {Math.abs(s.me) > 0.004 && <Money v={s.me} size={16} weight={800} color={b.color} />}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 16 }}>
                  <AvatarStack ids={g.members} size={26} max={5} />
                  <div style={{ fontSize: 12.5, color: T.ink2, fontWeight: 600 }}>
                    Total <span className="num" style={{ color: T.ink, fontWeight: 800 }}>{stbFmt(s.total)}</span>
                  </div>
                </div>
                <div style={{ marginTop: 14 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: T.ink3, fontWeight: 600, marginBottom: 6 }}>
                    <span>Settled</span><span className="num">{Math.round(s.progress * 100)}%</span>
                  </div>
                  <div style={{ height: 6, borderRadius: 3, background: T.surface2, overflow: 'hidden' }}>
                    <div className="grow" style={{ width: `${s.progress * 100}%`, height: '100%', borderRadius: 3, background: T.pos }} />
                  </div>
                </div>
              </Card>
            );
          })}
          <button className="dashed press" onClick={() => app.toast('New group created')}>
            <div style={{ width: 44, height: 44, borderRadius: 14, background: T.surface2, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name="group_add" size={22} /></div>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontSize: 15, fontWeight: 700 }}>Create a group</div>
              <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500 }}>For a trip, a flat, or a night out</div>
            </div>
          </button>
        </div>
      </ScreenBody>
      <Fab onClick={() => app.push({ name: 'add' })} />
    </>
  );
}

// ── GROUP DETAIL ──────────────────────────────────────────
function GroupScreen({ app, route }) {
  const g = app.groups.find((x) => x.id === route.id);
  const stats = useGroupStats(app)[g.id];
  const [tab, setTab] = React.useState(route.tab || 'expenses');
  const ex = app.expenses.filter((e) => e.group === g.id).sort((a, b) => b.date.localeCompare(a.date));
  const b = balanceLine(stats.me);

  return (
    <>
      <ScreenBody pb={120}>
        <TopBar onBack={app.back} title="" right={<><IconBtn name="person_add" /><IconBtn name="more_vert" /></>} />
        <div style={{ padding: '0 20px', display: 'flex', alignItems: 'center', gap: 16 }}>
          <GroupTile group={g} size={64} radius={20} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.6 }}>{g.name}</div>
            <div style={{ fontSize: 13, color: T.ink3, fontWeight: 600, marginTop: 2 }}>{g.kind} · {g.dates}</div>
          </div>
        </div>
        <div style={{ padding: '18px 16px 0' }}>
          <Card pad={18}>
            <div style={{ display: 'flex' }}>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600 }}>Group spend</div>
                <div className="num" style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.6, marginTop: 2 }}>{stbFmt(stats.total)}</div>
              </div>
              <div style={{ width: 1, background: T.line, margin: '0 16px' }} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600 }}>Your balance</div>
                <div className="num" style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.6, marginTop: 2, color: b.color }}>{stbFmt(stats.me, true)}</div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
              <button className="btn-dark" style={{ flex: 1 }} onClick={() => setTab('balances')}><Icon name="handshake" size={20} />Settle up</button>
              <button className="btn-tonal" style={{ flex: 1 }} onClick={() => app.toast('Group summary exported')}><Icon name="ios_share" size={20} />Export</button>
            </div>
          </Card>
        </div>
        <div style={{ padding: '18px 16px 14px' }}>
          <Segmented value={tab} onChange={setTab} options={[{ value: 'expenses', label: 'Expenses' }, { value: 'balances', label: 'Balances' }, { value: 'members', label: 'Members' }]} />
        </div>

        <div key={tab} className="fade-up" style={{ padding: '0 16px' }}>
          {tab === 'expenses' && (
            <Card pad={6}>
              {ex.map((e, i) => <ExpenseRow key={e.id} e={e} app={app} divider={i > 0} showAvatars />)}
            </Card>
          )}
          {tab === 'balances' && <GroupBalances app={app} g={g} stats={stats} />}
          {tab === 'members' && <GroupMembers app={app} g={g} stats={stats} />}
        </div>
      </ScreenBody>
      {tab === 'expenses' && <Fab onClick={() => app.push({ name: 'add', group: g.id })} bottom={24} />}
    </>
  );
}

function GroupMembers({ app, g, stats }) {
  const ex = app.expenses.filter((e) => e.group === g.id);
  const rows = g.members.map((id) => {
    const paid = ex.filter((e) => e.paidBy === id).reduce((s, e) => s + e.amount, 0);
    const share = ex.reduce((s, e) => s + (e.shares[id] || 0), 0);
    return { id, paid: r2(paid), share: r2(share), net: stats.net[id] || 0 };
  });
  const max = Math.max(...rows.map((r) => Math.max(r.paid, r.share)), 1);
  return (
    <Card pad={6}>
      <div style={{ display: 'flex', gap: 16, padding: '12px 14px 4px', fontSize: 12, fontWeight: 600, color: T.ink3 }}>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 10, height: 10, borderRadius: 3, background: T.ink }} />Paid</span>
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 10, height: 10, borderRadius: 3, background: T.surface2, boxShadow: `inset 0 0 0 1.5px ${T.ink3}` }} />Their share</span>
      </div>
      {rows.map((r, i) => {
        const bl = balanceLine(r.id === 'me' ? r.net : -r.net);
        return (
          <div key={r.id} style={{ padding: '14px 10px', margin: '0 4px', borderTop: i ? `1px solid ${T.line}` : 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Avatar id={r.id} size={38} />
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 700 }}>{r.id === 'me' ? 'You' : STB_PEOPLE[r.id].name}</div>
                <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500 }}>Paid <span className="num">{stbFmt(r.paid)}</span> · share <span className="num">{stbFmt(r.share)}</span></div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div className="num" style={{ fontSize: 15, fontWeight: 800, color: r.net > 0.004 ? T.pos : r.net < -0.004 ? T.neg : T.ink3 }}>{stbFmt(r.net, true)}</div>
                <div style={{ fontSize: 11, color: T.ink3, fontWeight: 600 }}>{r.net > 0.004 ? 'gets back' : r.net < -0.004 ? 'owes' : 'even'}</div>
              </div>
            </div>
            <div style={{ marginTop: 10, marginLeft: 50, display: 'flex', flexDirection: 'column', gap: 4 }}>
              <div style={{ height: 6, borderRadius: 3, background: T.ink, width: `${(r.paid / max) * 100}%`, minWidth: r.paid ? 6 : 0 }} className="grow" />
              <div style={{ height: 6, borderRadius: 3, background: T.surface2, boxShadow: `inset 0 0 0 1.5px ${T.ink3}`, width: `${(r.share / max) * 100}%` }} className="grow" />
            </div>
          </div>
        );
      })}
    </Card>
  );
}

Object.assign(window, { HomeScreen, GroupsScreen, GroupScreen, ExpenseRow, useGroupStats, useTotals, balanceLine, GroupMembers });
