type PlaceholderPageProps = {
  title: string
}

export function PlaceholderPage({ title }: PlaceholderPageProps) {
  return (
    <section>
      <h2 className="page-title">{title}</h2>
      <p>この画面は現在準備中です。</p>
    </section>
  )
}
